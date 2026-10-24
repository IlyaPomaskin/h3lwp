#!/usr/bin/env python3
"""Drive the Android wallpaper's debug commands over adb (see docs/debug-commands.md).

Interactive:      tools/debug_tui.py [-s SERIAL]
Non-interactive:  tools/debug_tui.py [-s SERIAL] send <CMD> [extras...]
                  e.g. tools/debug_tui.py send CAMERA_PAN --ei dx 1 --ei dy 0
"""

import argparse
import curses
import json
import queue
import re
import shlex
import subprocess
import sys
import threading
import time
from dataclasses import dataclass, field, replace
from enum import Enum, auto
from typing import Any, Callable, Optional, Sequence

PACKAGE = "com.homm3.livewallpaper"
RECEIVER_COMPONENT = f"{PACKAGE}/.debug.DebugCommandReceiver"
ACTION_PREFIX = f"{PACKAGE}.debug."
ADB_EXECUTABLE = "adb"
ADB_TIMEOUT_SECONDS = 8

CMD_PING = "PING"
CMD_MAP_NEXT = "MAP_NEXT"
CMD_MAP_RANDOM_VIEW = "MAP_RANDOM_VIEW"
CMD_MAP_TOGGLE_UNDERGROUND = "MAP_TOGGLE_UNDERGROUND"
CMD_CAMERA_PAN_SCREEN = "CAMERA_PAN_SCREEN"
CMD_CAMERA_ZOOM = "CAMERA_ZOOM"
CMD_PREFS_SET = "PREFS_SET"
CMD_STATE_GET = "STATE_GET"
CMD_ENGINE_REFRESH = "ENGINE_REFRESH"

EXTRA_STRING = "--es"
EXTRA_INT = "--ei"
EXTRA_BOOL = "--ez"
EXTRA_FLOAT = "--ef"

DIRECTION_KEY = "direction"
DIRECTION_FORWARD = "forward"
DIRECTION_BACKWARD = "backward"
PAN_FX_KEY = "fx"
PAN_FY_KEY = "fy"
PAN_STEP_SCREENS = 0.75
ZOOM_ACTION_KEY = "action"
ZOOM_IN = "in"
ZOOM_OUT = "out"
ZOOM_RESET = "reset"

PREF_SCALE = "scale"
PREF_MAP_UPDATE_INTERVAL = "mapUpdateInterval"
PREF_USE_SCROLL = "useScroll"
PREF_BRIGHTNESS = "brightness"
PREFS_KEY = "prefs"
ERROR_KEY = "error"

SCALE_MIN = 1
SCALE_MAX = 5
DEFAULT_SCALE = 4
MAP_UPDATE_INTERVALS = ("EVERY_SWITCH", "MINUTES_10", "MINUTES_30", "HOURS_2", "HOURS_24")
DEFAULT_MAP_UPDATE_INTERVAL = "EVERY_SWITCH"
DEFAULT_USE_SCROLL = True
BRIGHTNESS_MIN = 0.0
BRIGHTNESS_MAX = 1.0
BRIGHTNESS_STEP = 0.05
BRIGHTNESS_DECIMALS = 2
DEFAULT_BRIGHTNESS = 0.6

BROADCAST_MARKER = "Broadcast completed:"
RESULT_CODE_PATTERN = re.compile(r"result=(-?\d+)")
DATA_MARKER = 'data="'
ESCAPED_CHAR_PATTERN = re.compile(r"\\(.)")
SUCCESS_CODE = 0
MISSING_DATA_ERROR = "reply has no data (debug receiver not installed?)"

EXIT_SUCCESS = 0
EXIT_COMMAND_FAILED = 1
EXIT_TRANSPORT_ERROR = 2

STATE_POLL_INTERVAL_SECONDS = 2.0
INPUT_POLL_MILLISECONDS = 100
TWO_COLUMN_MIN_WIDTH = 80
HELP_COLUMN_WIDTH = 38
COLUMN_GAP = 2
STATUS_LINE_COUNT = 2
SELECTED_MARKER = "> "
UNSELECTED_MARKER = "  "
PENDING_MARKER = " ..."
NO_VALUE = "-"

KEY_ENTER_CODES = (10, 13, curses.KEY_ENTER)
KEY_TAB = 9


@dataclass(frozen=True)
class Extra:
    type_flag: str
    key: str
    value: Any

    def to_args(self) -> list[str]:
        return [self.type_flag, self.key, format_extra_value(self.value)]


@dataclass(frozen=True)
class Reply:
    result_code: Optional[int] = None
    data: Optional[dict] = None
    transport_error: Optional[str] = None

    @property
    def is_success(self) -> bool:
        return self.transport_error is None and self.result_code == SUCCESS_CODE

    @property
    def is_transport_failure(self) -> bool:
        return self.transport_error is not None

    def to_json(self) -> dict:
        if self.transport_error is not None:
            return {ERROR_KEY: self.transport_error}
        if self.data is None:
            return {}
        return self.data

    def describe(self) -> str:
        if self.transport_error is not None:
            return f"adb error: {self.transport_error}"
        if self.is_success:
            return f"OK {json.dumps(self.to_json())}"
        return f"FAIL result={self.result_code}: {self.error_message()}"

    def error_message(self) -> str:
        if self.transport_error is not None:
            return self.transport_error
        has_error_field = isinstance(self.data, dict) and ERROR_KEY in self.data
        if has_error_field:
            return str(self.data[ERROR_KEY])
        return f"result={self.result_code}"


@dataclass(frozen=True)
class Request:
    command: str
    extras: tuple[Extra, ...] = ()
    is_background_poll: bool = False

    def extra_args(self) -> list[str]:
        return [arg for extra in self.extras for arg in extra.to_args()]

    def describe(self) -> str:
        details = " ".join(f"{extra.key}={format_extra_value(extra.value)}" for extra in self.extras)
        return f"{self.command} {details}".strip()


def format_extra_value(value: Any) -> str:
    if isinstance(value, bool):
        return str(value).lower()
    return str(value)


def adb_base_args(serial: Optional[str]) -> list[str]:
    if not serial:
        return [ADB_EXECUTABLE]
    return [ADB_EXECUTABLE, "-s", serial]


def build_broadcast_args(command: str, extra_args: Sequence[str], serial: Optional[str]) -> list[str]:
    if not command:
        raise ValueError("command must not be empty")
    remote_args = ["am", "broadcast", "-n", RECEIVER_COMPONENT, "-a", ACTION_PREFIX + command, *extra_args]
    return [*adb_base_args(serial), "shell", *(shlex.quote(arg) for arg in remote_args)]


def unescape(text: str) -> str:
    return ESCAPED_CHAR_PATTERN.sub(r"\1", text)


def decode_leading_json_object(text: str) -> Optional[dict]:
    try:
        value, _ = json.JSONDecoder().raw_decode(text)
    except ValueError:
        return None
    if not isinstance(value, dict):
        return None
    return value


def parse_data(raw_data: str) -> Optional[dict]:
    plain = decode_leading_json_object(raw_data)
    if plain is not None:
        return plain
    return decode_leading_json_object(unescape(raw_data))


def find_broadcast_line(output: str) -> Optional[str]:
    return next((line for line in output.splitlines() if BROADCAST_MARKER in line), None)


def last_non_empty_line(text: str) -> str:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    if not lines:
        return ""
    return lines[-1]


def parse_broadcast_output(output: str) -> Reply:
    line = find_broadcast_line(output)
    if line is None:
        return Reply(transport_error=f"no broadcast reply: {last_non_empty_line(output) or 'empty output'}")
    code_match = RESULT_CODE_PATTERN.search(line)
    if code_match is None:
        return Reply(transport_error=f"no result code in: {line.strip()}")
    result_code = int(code_match.group(1))
    data_start = line.find(DATA_MARKER)
    if data_start < 0:
        return Reply(result_code=result_code, transport_error=MISSING_DATA_ERROR)
    raw_data = line[data_start + len(DATA_MARKER):]
    data = parse_data(raw_data)
    if data is None:
        return Reply(result_code=result_code, transport_error=f"unparseable data: {raw_data.strip()}")
    return Reply(result_code=result_code, data=data)


def run_command(command: str, extra_args: Sequence[str], serial: Optional[str]) -> Reply:
    args = build_broadcast_args(command, extra_args, serial)
    try:
        completed = subprocess.run(args, capture_output=True, text=True, timeout=ADB_TIMEOUT_SECONDS)
    except FileNotFoundError:
        return Reply(transport_error=f"'{ADB_EXECUTABLE}' not found in PATH")
    except subprocess.TimeoutExpired:
        return Reply(transport_error=f"adb timed out after {ADB_TIMEOUT_SECONDS}s")
    except OSError as error:
        return Reply(transport_error=str(error))
    has_broadcast_reply = BROADCAST_MARKER in completed.stdout
    if has_broadcast_reply:
        return parse_broadcast_output(completed.stdout)
    details = last_non_empty_line(completed.stderr) or last_non_empty_line(completed.stdout)
    return Reply(transport_error=details or f"adb exited with code {completed.returncode}")


def run_request(request: Request, serial: Optional[str]) -> Reply:
    return run_command(request.command, request.extra_args(), serial)


def clamp(value: float, low: float, high: float) -> float:
    return max(low, min(high, value))


def step_scale(current: Any, delta: int) -> int:
    return int(clamp(int(current) + delta, SCALE_MIN, SCALE_MAX))


def step_interval(current: Any, delta: int) -> str:
    if current not in MAP_UPDATE_INTERVALS:
        return DEFAULT_MAP_UPDATE_INTERVAL
    index = MAP_UPDATE_INTERVALS.index(current)
    return MAP_UPDATE_INTERVALS[(index + delta) % len(MAP_UPDATE_INTERVALS)]


def toggle_bool(current: Any, _delta: int) -> bool:
    return not bool(current)


def step_brightness(current: Any, delta: int) -> float:
    stepped = float(current) + delta * BRIGHTNESS_STEP
    return round(clamp(stepped, BRIGHTNESS_MIN, BRIGHTNESS_MAX), BRIGHTNESS_DECIMALS)


@dataclass(frozen=True)
class PrefField:
    key: str
    type_flag: str
    default: Any
    adjust: Callable[[Any, int], Any]


PREF_FIELDS = (
    PrefField(PREF_SCALE, EXTRA_INT, DEFAULT_SCALE, step_scale),
    PrefField(PREF_MAP_UPDATE_INTERVAL, EXTRA_STRING, DEFAULT_MAP_UPDATE_INTERVAL, step_interval),
    PrefField(PREF_USE_SCROLL, EXTRA_BOOL, DEFAULT_USE_SCROLL, toggle_bool),
    PrefField(PREF_BRIGHTNESS, EXTRA_FLOAT, DEFAULT_BRIGHTNESS, step_brightness),
)


def build_pref_request(pref: PrefField, prefs: dict, delta: int) -> tuple[Request, dict]:
    new_value = pref.adjust(prefs.get(pref.key, pref.default), delta)
    request = Request(CMD_PREFS_SET, (Extra(pref.type_flag, pref.key, new_value),))
    return request, {**prefs, pref.key: new_value}


def extract_prefs(data: Optional[dict]) -> Optional[dict]:
    if not isinstance(data, dict):
        return None
    nested = data.get(PREFS_KEY)
    if isinstance(nested, dict):
        return nested
    has_pref_keys = any(pref.key in data for pref in PREF_FIELDS)
    if has_pref_keys:
        return data
    return None


@dataclass(frozen=True)
class CommandBinding:
    keys: tuple[int, ...]
    key_label: str
    description: str
    request: Request


class LocalAction(Enum):
    REFRESH_STATE = auto()
    QUIT = auto()
    SELECT_NEXT = auto()
    SELECT_PREVIOUS = auto()
    DECREASE = auto()
    INCREASE = auto()


@dataclass(frozen=True)
class LocalBinding:
    keys: tuple[int, ...]
    key_label: str
    description: str
    action: LocalAction


def pan(fraction_x: float, fraction_y: float) -> Request:
    return Request(CMD_CAMERA_PAN_SCREEN, (Extra(EXTRA_FLOAT, PAN_FX_KEY, fraction_x), Extra(EXTRA_FLOAT, PAN_FY_KEY, fraction_y)))


def zoom(action: str) -> Request:
    return Request(CMD_CAMERA_ZOOM, (Extra(EXTRA_STRING, ZOOM_ACTION_KEY, action),))


def map_next(direction: str) -> Request:
    return Request(CMD_MAP_NEXT, (Extra(EXTRA_STRING, DIRECTION_KEY, direction),))


COMMAND_BINDINGS = (
    CommandBinding((ord("]"),), "]", "next map", map_next(DIRECTION_FORWARD)),
    CommandBinding((ord("["),), "[", "previous map", map_next(DIRECTION_BACKWARD)),
    CommandBinding((ord("u"),), "u", "toggle underground", Request(CMD_MAP_TOGGLE_UNDERGROUND)),
    CommandBinding(KEY_ENTER_CODES, "Enter", "random view", Request(CMD_MAP_RANDOM_VIEW)),
    CommandBinding((curses.KEY_UP,), "Up", "pan up", pan(0, -PAN_STEP_SCREENS)),
    CommandBinding((curses.KEY_DOWN,), "Down", "pan down", pan(0, PAN_STEP_SCREENS)),
    CommandBinding((curses.KEY_LEFT,), "Left", "pan left", pan(-PAN_STEP_SCREENS, 0)),
    CommandBinding((curses.KEY_RIGHT,), "Right", "pan right", pan(PAN_STEP_SCREENS, 0)),
    CommandBinding((ord("="),), "=", "zoom in", zoom(ZOOM_IN)),
    CommandBinding((ord("-"),), "-", "zoom out", zoom(ZOOM_OUT)),
    CommandBinding((ord("0"),), "0", "zoom reset", zoom(ZOOM_RESET)),
    CommandBinding((ord("r"),), "r", "engine refresh", Request(CMD_ENGINE_REFRESH)),
    CommandBinding((ord("p"),), "p", "ping", Request(CMD_PING)),
)

LOCAL_BINDINGS = (
    LocalBinding((ord("s"),), "s", "refresh state", LocalAction.REFRESH_STATE),
    LocalBinding((KEY_TAB, ord("j")), "Tab/j", "next pref", LocalAction.SELECT_NEXT),
    LocalBinding((curses.KEY_BTAB, ord("k")), "S-Tab/k", "previous pref", LocalAction.SELECT_PREVIOUS),
    LocalBinding((ord("h"), ord(",")), "h/,", "decrease pref", LocalAction.DECREASE),
    LocalBinding((ord("l"), ord(".")), "l/.", "increase pref", LocalAction.INCREASE),
    LocalBinding((ord("q"),), "q", "quit", LocalAction.QUIT),
)

COMMAND_BY_KEY = {key: binding.request for binding in COMMAND_BINDINGS for key in binding.keys}
LOCAL_ACTION_BY_KEY = {key: binding.action for binding in LOCAL_BINDINGS for key in binding.keys}
STATE_REQUEST = Request(CMD_STATE_GET, is_background_poll=True)


class CommandWorker:
    def __init__(self, serial: Optional[str]):
        self._serial = serial
        self._requests: "queue.Queue[Request]" = queue.Queue()
        self._replies: "queue.Queue[tuple[Request, Reply]]" = queue.Queue()
        threading.Thread(target=self._loop, daemon=True).start()

    def submit(self, request: Request) -> None:
        self._requests.put(request)

    def drain(self) -> list[tuple[Request, Reply]]:
        drained = []
        while not self._replies.empty():
            drained.append(self._replies.get_nowait())
        return drained

    def _loop(self) -> None:
        while True:
            request = self._requests.get()
            self._replies.put((request, run_request(request, self._serial)))


@dataclass(frozen=True)
class AppState:
    serial: Optional[str]
    is_running: bool = True
    pending_count: int = 0
    last_poll_time: float = 0.0
    selected_pref_index: int = 0
    prefs: dict = field(default_factory=dict)
    engine_state: Optional[dict] = None
    state_error: Optional[str] = None
    last_request: Optional[Request] = None
    last_reply: Optional[Reply] = None


def submit(app: AppState, worker: CommandWorker, request: Request) -> AppState:
    worker.submit(request)
    return replace(app, pending_count=app.pending_count + 1)


def submit_user_request(app: AppState, worker: CommandWorker, request: Request) -> AppState:
    return replace(submit(app, worker, request), last_request=request, last_reply=None)


def adjust_selected_pref(app: AppState, worker: CommandWorker, delta: int) -> AppState:
    pref = PREF_FIELDS[app.selected_pref_index]
    request, optimistic_prefs = build_pref_request(pref, app.prefs, delta)
    return replace(submit_user_request(app, worker, request), prefs=optimistic_prefs)


def select_pref(app: AppState, delta: int) -> AppState:
    return replace(app, selected_pref_index=(app.selected_pref_index + delta) % len(PREF_FIELDS))


def apply_local_action(app: AppState, worker: CommandWorker, action: LocalAction) -> AppState:
    handlers = {
        LocalAction.QUIT: lambda: replace(app, is_running=False),
        LocalAction.REFRESH_STATE: lambda: submit_user_request(app, worker, Request(CMD_STATE_GET)),
        LocalAction.SELECT_NEXT: lambda: select_pref(app, 1),
        LocalAction.SELECT_PREVIOUS: lambda: select_pref(app, -1),
        LocalAction.DECREASE: lambda: adjust_selected_pref(app, worker, -1),
        LocalAction.INCREASE: lambda: adjust_selected_pref(app, worker, 1),
    }
    return handlers[action]()


def handle_key(app: AppState, worker: CommandWorker, key: int) -> AppState:
    request = COMMAND_BY_KEY.get(key)
    if request is not None:
        return submit_user_request(app, worker, request)
    action = LOCAL_ACTION_BY_KEY.get(key)
    if action is not None:
        return apply_local_action(app, worker, action)
    return app


def apply_state_reply(app: AppState, reply: Reply) -> AppState:
    if not reply.is_success:
        return replace(app, engine_state=None, state_error=reply.error_message())
    prefs = extract_prefs(reply.data) or app.prefs
    return replace(app, engine_state=reply.data, state_error=None, prefs=prefs)


def apply_command_reply(app: AppState, request: Request, reply: Reply) -> AppState:
    prefs = extract_prefs(reply.data) if reply.is_success else None
    return replace(app, last_request=request, last_reply=reply, prefs=prefs or app.prefs)


def apply_reply(app: AppState, worker: CommandWorker, request: Request, reply: Reply) -> AppState:
    settled = replace(app, pending_count=app.pending_count - 1, last_poll_time=time.monotonic())
    if request.command == CMD_STATE_GET:
        with_state = apply_state_reply(settled, reply)
        if request.is_background_poll:
            return with_state
        return apply_command_reply(with_state, request, reply)
    return submit(apply_command_reply(settled, request, reply), worker, STATE_REQUEST)


def apply_replies(app: AppState, worker: CommandWorker) -> AppState:
    current = app
    for request, reply in worker.drain():
        current = apply_reply(current, worker, request, reply)
    return current


def poll_state_if_idle(app: AppState, worker: CommandWorker) -> AppState:
    is_idle = app.pending_count == 0
    is_poll_due = time.monotonic() - app.last_poll_time >= STATE_POLL_INTERVAL_SECONDS
    if not (is_idle and is_poll_due):
        return app
    return replace(submit(app, worker, STATE_REQUEST), last_poll_time=time.monotonic())


@dataclass(frozen=True)
class Region:
    top: int
    left: int
    width: int


@dataclass(frozen=True)
class Panel:
    title: str
    lines: tuple[str, ...]


def safe_addstr(screen, top: int, left: int, text: str, attr: int = curses.A_NORMAL) -> None:
    height, width = screen.getmaxyx()
    is_outside = top < 0 or top >= height or left < 0 or left >= width
    if is_outside:
        return
    try:
        screen.addnstr(top, left, text, width - left, attr)
    except curses.error:
        pass


def draw_panel(screen, region: Region, panel: Panel) -> int:
    safe_addstr(screen, region.top, region.left, panel.title[: region.width], curses.A_BOLD)
    for offset, line in enumerate(panel.lines, start=1):
        safe_addstr(screen, region.top + offset, region.left, line[: region.width])
    return len(panel.lines) + 2


def help_panel() -> Panel:
    bindings = [*COMMAND_BINDINGS, *LOCAL_BINDINGS]
    return Panel("Keys", tuple(f"{binding.key_label:>8}  {binding.description}" for binding in bindings))


def format_camera(camera: Any) -> str:
    if not isinstance(camera, dict):
        return NO_VALUE
    return f"x={camera.get('x', NO_VALUE)} y={camera.get('y', NO_VALUE)}"


def state_panel(app: AppState) -> Panel:
    title = f"State{PENDING_MARKER if app.pending_count else ''}"
    if app.state_error is not None:
        return Panel(title, (f"error: {app.state_error}",))
    if app.engine_state is None:
        return Panel(title, ("waiting...",))
    state = app.engine_state
    return Panel(title, (
        f"screen      {state.get('screen', NO_VALUE)}",
        f"map         {state.get('map', NO_VALUE)}",
        f"underground {state.get('underground', NO_VALUE)}",
        f"zoom        {state.get('zoom', NO_VALUE)}",
        f"camera      {format_camera(state.get('camera'))}",
    ))


def prefs_panel(app: AppState) -> Panel:
    def line(index: int, pref: PrefField) -> str:
        marker = SELECTED_MARKER if index == app.selected_pref_index else UNSELECTED_MARKER
        value = format_extra_value(app.prefs.get(pref.key, NO_VALUE))
        return f"{marker}{pref.key:<18} {value}"

    return Panel("Prefs (Tab/j/k select, h/l change)", tuple(line(i, pref) for i, pref in enumerate(PREF_FIELDS)))


def status_lines(app: AppState) -> tuple[str, str]:
    device = app.serial or "default device"
    if app.last_request is None:
        return f"[{device}] no command sent yet", ""
    reply_text = "waiting..."
    if app.last_reply is not None:
        reply_text = app.last_reply.describe()
    return f"[{device}] last: {app.last_request.describe()}", reply_text


def draw_status(screen, app: AppState) -> None:
    height, _ = screen.getmaxyx()
    command_line, reply_line = status_lines(app)
    is_failure = app.last_reply is not None and not app.last_reply.is_success
    reply_attr = curses.A_BOLD if is_failure else curses.A_NORMAL
    safe_addstr(screen, height - STATUS_LINE_COUNT, 0, command_line, curses.A_REVERSE)
    safe_addstr(screen, height - 1, 0, reply_line, reply_attr)


def draw(screen, app: AppState) -> None:
    screen.erase()
    _, width = screen.getmaxyx()
    is_two_columns = width >= TWO_COLUMN_MIN_WIDTH
    if is_two_columns:
        draw_panel(screen, Region(0, 0, HELP_COLUMN_WIDTH), help_panel())
        right_left = HELP_COLUMN_WIDTH + COLUMN_GAP
        right_region = Region(0, right_left, width - right_left)
        prefs_top = draw_panel(screen, right_region, state_panel(app))
        draw_panel(screen, replace(right_region, top=prefs_top), prefs_panel(app))
    else:
        state_height = draw_panel(screen, Region(0, 0, width), state_panel(app))
        prefs_height = draw_panel(screen, Region(state_height, 0, width), prefs_panel(app))
        draw_panel(screen, Region(state_height + prefs_height, 0, width), help_panel())
    draw_status(screen, app)
    screen.refresh()


def hide_cursor() -> None:
    try:
        curses.curs_set(0)
    except curses.error:
        pass


def run_tui(screen, serial: Optional[str]) -> None:
    hide_cursor()
    screen.timeout(INPUT_POLL_MILLISECONDS)
    worker = CommandWorker(serial)
    app = AppState(serial=serial)
    while app.is_running:
        app = poll_state_if_idle(apply_replies(app, worker), worker)
        draw(screen, app)
        key = screen.getch()
        if key >= 0:
            app = handle_key(app, worker, key)


def run_send(serial: Optional[str], command: str, extra_args: Sequence[str]) -> int:
    reply = run_command(command.upper(), extra_args, serial)
    print(json.dumps(reply.to_json()))
    if reply.is_transport_failure:
        print(reply.error_message(), file=sys.stderr)
        return EXIT_TRANSPORT_ERROR
    if reply.is_success:
        return EXIT_SUCCESS
    return EXIT_COMMAND_FAILED


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Drive h3lwp debug commands over adb.")
    parser.add_argument("-s", "--serial", help="adb device serial (passed as adb -s)")
    subparsers = parser.add_subparsers(dest="mode")
    send_parser = subparsers.add_parser(
        "send",
        help="send one command, print JSON reply; exit 1 on result!=0, 2 on adb error",
    )
    send_parser.add_argument("command", help="command name, e.g. PING, MAP_NEXT")
    send_parser.add_argument("extras", nargs=argparse.REMAINDER, help="am extras, e.g. --ei dx 1")
    return parser.parse_args(argv)


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    if args.mode == "send":
        return run_send(args.serial, args.command, args.extras)
    curses.wrapper(run_tui, args.serial)
    return EXIT_SUCCESS


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except KeyboardInterrupt:
        sys.exit(EXIT_SUCCESS)
