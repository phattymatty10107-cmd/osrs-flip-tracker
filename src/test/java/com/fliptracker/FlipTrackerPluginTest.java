package com.fliptracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches RuneLite with this plugin loaded. Run main in IntelliJ, or: gradle runClient */
public class FlipTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		// RuneLite's developer mode needs assertions on (-ea). If they're off, relaunch with them on.
		boolean assertionsOn = false;
		assert assertionsOn = true;
		if (!assertionsOn)
		{
			String java = ProcessHandle.current().info().command().orElse("java");
			List<String> cmd = new ArrayList<>(Arrays.asList(java, "-ea",
				"-cp", System.getProperty("java.class.path"), FlipTrackerPluginTest.class.getName()));
			cmd.addAll(Arrays.asList(args));
			Process process = new ProcessBuilder(cmd).inheritIO().start();
			System.exit(process.waitFor());
			return;
		}

		ExternalPluginManager.loadBuiltin(FlipTrackerPlugin.class);
		RuneLite.main(args);
	}
}
