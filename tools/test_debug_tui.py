#!/usr/bin/env python3
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import debug_tui as tui

RECEIVER = "com.homm3.livewallpaper/.debug.DebugCommandReceiver"
ACTION_PING = "com.homm3.livewallpaper.debug.PING"


class ParseBroadcastOutputTest(unittest.TestCase):
    def test_plain_json_success(self):
        reply = tui.parse_broadcast_output(
            'Broadcasting: Intent { act=x }\nBroadcast completed: result=0, data="{"pong":true}"\n'
        )
        self.assertTrue(reply.is_success)
        self.assertEqual(reply.data, {"pong": True})

    def test_escaped_quotes(self):
        reply = tui.parse_broadcast_output('Broadcast completed: result=0, data="{\\"map\\":\\"a \\\\\\"b\\\\\\"\\"}"')
        self.assertTrue(reply.is_success)
        self.assertEqual(reply.data, {"map": 'a "b"'})

    def test_nested_state_with_trailing_extras(self):
        output = (
            'Broadcast completed: result=0, data="{"screen":"GameScreen","camera":{"x":1.5,"y":0.0},'
            '"prefs":{"scale":2}}", extras: Bundle[{}]'
        )
        reply = tui.parse_broadcast_output(output)
        self.assertEqual(reply.data["camera"], {"x": 1.5, "y": 0.0})
        self.assertEqual(tui.extract_prefs(reply.data), {"scale": 2})

    def test_failure_reply(self):
        reply = tui.parse_broadcast_output('Broadcast completed: result=1, data="{"error":"engine not running"}"')
        self.assertFalse(reply.is_success)
        self.assertFalse(reply.is_transport_failure)
        self.assertEqual(reply.error_message(), "engine not running")

    def test_missing_data_means_receiver_absent(self):
        reply = tui.parse_broadcast_output("Broadcast completed: result=0")
        self.assertFalse(reply.is_success)
        self.assertTrue(reply.is_transport_failure)
        self.assertEqual(reply.result_code, 0)
        self.assertIn("not installed", reply.error_message())

    def test_unparseable_data(self):
        reply = tui.parse_broadcast_output('Broadcast completed: result=0, data="not json"')
        self.assertTrue(reply.is_transport_failure)
        self.assertFalse(reply.is_success)

    def test_no_broadcast_line(self):
        reply = tui.parse_broadcast_output("adb: no devices/emulators found\n")
        self.assertTrue(reply.is_transport_failure)
        self.assertIn("no devices", reply.error_message())

    def test_empty_output(self):
        self.assertTrue(tui.parse_broadcast_output("").is_transport_failure)


class BuildArgsTest(unittest.TestCase):
    def test_without_serial(self):
        self.assertEqual(
            tui.build_broadcast_args("PING", [], None),
            ["adb", "shell", "am", "broadcast", "-n", RECEIVER, "-a", ACTION_PING],
        )

    def test_with_serial_and_extras(self):
        args = tui.build_broadcast_args("CAMERA_PAN", ["--ei", "dx", "-1", "--ei", "dy", "0"], "ABC123")
        self.assertEqual(args[:4], ["adb", "-s", "ABC123", "shell"])
        self.assertEqual(args[-6:], ["--ei", "dx", "-1", "--ei", "dy", "0"])
        self.assertIn("com.homm3.livewallpaper.debug.CAMERA_PAN", args)

    def test_quotes_values_for_remote_shell(self):
        args = tui.build_broadcast_args("PREFS_SET", ["--es", "x", "a b"], None)
        self.assertEqual(args[-1], "'a b'")

    def test_empty_command_rejected(self):
        with self.assertRaises(ValueError):
            tui.build_broadcast_args("", [], None)

    def test_binding_requests(self):
        self.assertEqual(tui.COMMAND_BY_KEY[ord("[")].extra_args(), ["--es", "direction", "backward"])
        self.assertEqual(tui.COMMAND_BY_KEY[tui.curses.KEY_UP].extra_args(), ["--ef", "fx", "0", "--ef", "fy", "-0.75"])
        self.assertEqual(tui.COMMAND_BY_KEY[ord("0")].extra_args(), ["--es", "action", "reset"])

    def test_no_key_conflicts(self):
        command_keys = set(tui.COMMAND_BY_KEY)
        local_keys = set(tui.LOCAL_ACTION_BY_KEY)
        self.assertEqual(command_keys & local_keys, set())


class PrefRequestTest(unittest.TestCase):
    def fields(self):
        return {pref.key: pref for pref in tui.PREF_FIELDS}

    def test_scale_clamped(self):
        request, prefs = tui.build_pref_request(self.fields()["scale"], {"scale": 5}, 1)
        self.assertEqual(prefs["scale"], 5)
        self.assertEqual(request.extra_args(), ["--ei", "scale", "5"])

    def test_interval_wraps(self):
        _, prefs = tui.build_pref_request(self.fields()["mapUpdateInterval"], {"mapUpdateInterval": "HOURS_24"}, 1)
        self.assertEqual(prefs["mapUpdateInterval"], "EVERY_SWITCH")

    def test_bool_toggle_uses_default(self):
        request, _ = tui.build_pref_request(self.fields()["useScroll"], {}, -1)
        self.assertEqual(request.extra_args(), ["--ez", "useScroll", "false"])

    def test_brightness_step(self):
        request, prefs = tui.build_pref_request(self.fields()["brightness"], {"brightness": 0.6}, -1)
        self.assertEqual(prefs["brightness"], 0.55)
        self.assertEqual(request.extra_args(), ["--ef", "brightness", "0.55"])


class CliTest(unittest.TestCase):
    def test_send_extras_passthrough(self):
        args = tui.parse_args(["-s", "X", "send", "camera_pan", "--ei", "dx", "1"])
        self.assertEqual((args.mode, args.serial, args.command, args.extras), ("send", "X", "camera_pan", ["--ei", "dx", "1"]))


if __name__ == "__main__":
    unittest.main()
