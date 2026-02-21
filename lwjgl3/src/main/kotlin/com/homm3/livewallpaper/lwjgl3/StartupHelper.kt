/*
 * Copyright 2020 damios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Note, the above license and copyright applies to this file only.
package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.Version
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3NativesLoader

import org.lwjgl.system.JNI
import org.lwjgl.system.linux.UNISTD
import org.lwjgl.system.macosx.LibC
import org.lwjgl.system.macosx.ObjCRuntime

import java.io.File
import java.lang.management.ManagementFactory

/**
 * A helper object for game startup, featuring three utilities related to LWJGL3 on various operating systems.
 *
 * The utilities are as follows:
 * - Windows: Prevents a common crash related to LWJGL3's extraction of shared library files.
 * - macOS: Spawns a child JVM process with `-XstartOnFirstThread` in the JVM args (if it was not already). This is required for LWJGL3 to work on macOS.
 * - Linux (NVIDIA GPUs only): Spawns a child JVM process with the `__GL_THREADED_OPTIMIZATIONS` [Environment Variable][System.getenv] set to `0` (if it was not already). This is required for LWJGL3 to work on Linux with NVIDIA GPUs.
 *
 * [Based on this java-gaming.org post by kappa](https://jvm-gaming.org/t/starting-jvm-on-mac-with-xstartonfirstthread-programmatically/57547)
 * @author damios
 */
object StartupHelper {

	private const val JVM_RESTARTED_ARG = "jvmIsRestarted"

	/**
	 * Must only be called on Linux. Check OS first (or use short-circuit evaluation)!
	 * @return whether NVIDIA drivers are present on Linux.
	 */
	fun isLinuxNvidia(): Boolean = File("/proc/driver").list { _, path: String -> "NVIDIA" in path.uppercase() }.isNullOrEmpty().not()

	/**
	 * Applies the utilities as described by [StartupHelper]'s KDoc.
	 *
	 * All [Environment Variables][System.getenv] are copied to the child JVM process (if it is spawned), as specified by [ProcessBuilder.environment]; the same applies for [System Properties][System.getProperties].
	 *
	 * **Usage:**
	 *
	 * ```
	 * fun main() {
	 *   if (StartupHelper.startNewJvmIfRequired()) return
	 *   // ...
	 * }
	 * ```
	 * @param inheritIO whether I/O should be inherited in the child JVM process. Please note that enabling this will block the thread until the child JVM process stops executing.
	 * @return whether a child JVM process was spawned or not.
	 */
	@JvmOverloads
	fun startNewJvmIfRequired(inheritIO: Boolean = true): Boolean {
		val osName: String = System.getProperty("os.name").lowercase()
		if ("mac" in osName) return startNewJvm0(isMac = true, inheritIO)
		if ("windows" in osName) {
			// Here, we are trying to work around an issue with how LWJGL3 loads its extracted .dll files.
			// By default, LWJGL3 extracts to the directory specified by "java.io.tmpdir": usually, the user's home.
			// If the user's name has non-ASCII (or some non-alphanumeric) characters in it, that would fail.
			// By extracting to the relevant "ProgramData" folder, which is usually "C:\ProgramData", we avoid this.
			// We also temporarily change the "user.name" property to one without any chars that would be invalid.
			// We revert our changes immediately after loading LWJGL3 natives.
			val programData: String = System.getenv("ProgramData") ?: "C:\\Temp"
			val prevTmpDir: String = System.getProperty("java.io.tmpdir", programData)
			val prevUser: String = System.getProperty("user.name", "libGDX_User")
			System.setProperty("java.io.tmpdir", "$programData\\libGDX-temp")
			System.setProperty(
				"user.name",
				"User_${prevUser.hashCode()}_GDX${Version.VERSION}".replace('.', '_')
			)
			Lwjgl3NativesLoader.load()
			System.setProperty("java.io.tmpdir", prevTmpDir)
			System.setProperty("user.name", prevUser)
			return false
		}
		return startNewJvm0(isMac = false, inheritIO)
	}

	private const val MAC_JRE_ERR_MSG: String = "A Java installation could not be found. If you are distributing this app with a bundled JRE, be sure to set the '-XstartOnFirstThread' argument manually!"
	private const val LINUX_JRE_ERR_MSG: String = "A Java installation could not be found. If you are distributing this app with a bundled JRE, be sure to set the environment variable '__GL_THREADED_OPTIMIZATIONS' to '0'!"
	private const val CHILD_LOOP_ERR_MSG: String = "The current JVM process is a spawned child JVM process, but StartupHelper has attempted to spawn another child JVM process!  This is a broken state, and should not normally happen!  Your game may crash or not function properly!"

	/**
	 * Spawns a child JVM process if on macOS or NVIDIA Linux.
	 *
	 * All [Environment Variables][System.getenv] are copied to the child JVM process (if it is spawned), as specified by [ProcessBuilder.environment]; the same applies for [System Properties][System.getProperties].
	 *
	 * @param isMac whether the current OS is macOS. If this is `false` then the current OS is assumed to be Linux (and an immediate check for NVIDIA drivers is performed).
	 * @param inheritIO whether I/O should be inherited in the child JVM process. Please note that enabling this will block the thread until the child JVM process stops executing.
	 * @return whether a child JVM process was spawned or not.
	 */
	fun startNewJvm0(isMac: Boolean, inheritIO: Boolean): Boolean {
		val processID: Long = if (isMac) LibC.getpid() else UNISTD.getpid().toLong()
		if (!isMac) {
			// No need to restart non-NVIDIA Linux
			if (!isLinuxNvidia()) return false
			// check whether __GL_THREADED_OPTIMIZATIONS is already disabled
			if (System.getenv("__GL_THREADED_OPTIMIZATIONS") == "0") return false
		} else {
			// There is no need for -XstartOnFirstThread on Graal native image
			if (System.getProperty("org.graalvm.nativeimage.imagecode", "").isNotEmpty()) return false

			// Checks if we are already on the main thread, such as from running via Construo.
			val objcMsgSend: Long = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend")
			val nsThread: Long = ObjCRuntime.objc_getClass("NSThread")
			val currentThread: Long = JNI.invokePPP(nsThread, ObjCRuntime.sel_getUid("currentThread"), objcMsgSend)
			val isMainThread: Boolean = JNI.invokePPZ(currentThread, ObjCRuntime.sel_getUid("isMainThread"), objcMsgSend)
			if (isMainThread) return false

			if (System.getenv("JAVA_STARTED_ON_FIRST_THREAD_$processID") == "1") return false
		}

		// Check whether this JVM process is a child JVM process already.
		// This state shouldn't (usually) be reachable, but this stops us from endlessly spawning new child JVM processes.
		if (System.getProperty(JVM_RESTARTED_ARG) == "true") {
			System.err.println(CHILD_LOOP_ERR_MSG)
			return false
		}

		// Spawn the child JVM process with updated environment variables or JVM args
		val jvmArgs: MutableList<String> = mutableListOf()
		// The following line is used assuming you target Java 8, the minimum for LWJGL3.
		val javaExecPath = "${System.getProperty("java.home")}/bin/java"
		// If targeting Java 9 or higher, you could use the following instead of the above line:
		//val javaExecPath = ProcessHandle.current().info().command().orElseThrow()
		if (!File(javaExecPath).exists()) {
			System.err.println(if (isMac) MAC_JRE_ERR_MSG else LINUX_JRE_ERR_MSG)
			return false
		}

		jvmArgs += javaExecPath
		if (isMac) jvmArgs += "-XstartOnFirstThread"
		jvmArgs += "-D$JVM_RESTARTED_ARG=true"
		jvmArgs += ManagementFactory.getRuntimeMXBean().inputArguments
		jvmArgs += "-cp"
		jvmArgs += System.getProperty("java.class.path")
		jvmArgs += System.getenv("JAVA_MAIN_CLASS_$processID") ?: run {
			val trace: Array<StackTraceElement> = Thread.currentThread().stackTrace
			if (trace.isNotEmpty()) return@run trace[trace.lastIndex].className
			else {
				System.err.println("The main class could not be determined through stacktrace.")
				return false
			}
		}

		try {
			val processBuilder = ProcessBuilder(jvmArgs)
			if (!isMac) processBuilder.environment()["__GL_THREADED_OPTIMIZATIONS"] = "0"

			if (!inheritIO) processBuilder.start()
			else processBuilder.inheritIO().start().waitFor()
		} catch (e: Exception) {
			System.err.println("There was a problem restarting the JVM.")
			e.printStackTrace()
		}

		return true
	}
}