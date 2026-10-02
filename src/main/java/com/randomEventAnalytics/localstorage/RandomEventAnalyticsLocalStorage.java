/*
 * Copyright (c) 2018
 * 	TheStonedTurtle <https://github.com/TheStonedTurtle>, zmanowar <https://github.com/zmanowar>
 * All rights reserved.
 *
 * Modified source from https://github.com/TheStonedTurtle/Loot-Logger/
 */
package com.randomEventAnalytics.localstorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.client.util.Filepath;
import net.runelite.http.api.RuneLiteAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Either break the random events into seperate files
 * or implement/import a DB system.
 **/
public class RandomEventAnalyticsLocalStorage
{
	private static final String FILE_EXTENSION = ".log";
	private static final String RANDOM_EVENTS_FILE = "random-events";
	private static final Logger log = LoggerFactory.getLogger(RandomEventAnalyticsLocalStorage.class);
	private Filepath pluginDirectory;
	private Filepath playerFolder;
	@Getter
	private int numberOfLoggedEvents = 0;
	@Getter
	private String accountHash;

	@Inject
	public RandomEventAnalyticsLocalStorage()
	{
	}

	public void initialize(Filepath pluginDirectory) throws IOException
	{
		this.pluginDirectory = pluginDirectory;
		pluginDirectory.createDirectories();
	}

	public boolean setPlayerAccountHash(final String accountHash)
	{
		if (pluginDirectory == null)
		{
			throw new IllegalStateException("Plugin storage directory has not been initialized");
		}

		if (accountHash.equalsIgnoreCase(this.accountHash))
		{
			return false;
		}

		try
		{
			playerFolder = pluginDirectory.joinSegment(accountHash);
			playerFolder.createDirectories();
		}
		catch (IllegalArgumentException | IOException e)
		{
			throw new IllegalStateException("Unable to initialize storage for account " + accountHash, e);
		}
		this.accountHash = accountHash;
		return true;
	}

	private Filepath getFile(String fileName)
	{
		if (playerFolder == null)
		{
			throw new IllegalStateException("Player storage directory has not been initialized");
		}

		return playerFolder.joinSegment(fileName + FILE_EXTENSION);
	}

	public synchronized ArrayList<RandomEventRecord> loadRandomEventRecords()
	{
		final Filepath file = getFile(RANDOM_EVENTS_FILE);
		final ArrayList<RandomEventRecord> data = new ArrayList<>();

		if (!file.exists())
		{
			numberOfLoggedEvents = 0;
			return data;
		}

		try (final BufferedReader br = file.openBufferedReader())
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				// Skips the empty line at end of file
				if (line.length() > 0)
				{
					final RandomEventRecord r = RuneLiteAPI.GSON.fromJson(line, RandomEventRecord.class);
					data.add(r);
				}
			}

		}
		catch (IOException e)
		{
			log.warn("IOException for file {}: {}", file.getFileName(), e.getMessage());
		}

		numberOfLoggedEvents = data.size();
		return data;
	}

	public synchronized RandomEventRecord getMostRecentRandom()
	{
		final ArrayList<RandomEventRecord> data = loadRandomEventRecords();
		if (data.size() > 0)
		{
			return data.get(data.size() - 1);
		}

		return null;
	}

	public synchronized boolean renameUsernameFolderToAccountHash(final String username, final long hash)
	{
		if (pluginDirectory == null)
		{
			throw new IllegalStateException("Plugin storage directory has not been initialized");
		}

		final Filepath usernameDir;
		try
		{
			usernameDir = pluginDirectory.joinSegment(username);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Unable to migrate legacy data directory for username '{}'", username);
			return false;
		}

		if (!usernameDir.exists())
		{
			return true;
		}

		final Filepath hashDir = pluginDirectory.joinSegment(String.valueOf(hash));
		if (hashDir.exists())
		{
			log.warn("Can't rename username folder to account hash as the folder for this account hash already exists" + "." + " This was most likely caused by running RL through the Jagex launcher before the migration code" + " was" + " added");
			log.warn("Username: {} | AccountHash: {}", username, hash);
			return false;
		}

		try
		{
			usernameDir.moveTo(hashDir);
			return true;
		}
		catch (IOException e)
		{
			log.warn("Unable to migrate username directory '{}' to account hash {}: {}", username, hash, e.getMessage());
			return false;
		}
	}

	public synchronized boolean addRandomEventRecord(RandomEventRecord rec)
	{
		final Filepath randomEventsFile = getFile(RANDOM_EVENTS_FILE);

		// Convert entry to JSON
		final String dataAsString = RuneLiteAPI.GSON.toJson(rec);

		// Open File in append mode and write new data
		try (BufferedWriter file = randomEventsFile.openBufferedWriter(
			StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND))
		{
			file.append(dataAsString);
			file.newLine();
			numberOfLoggedEvents += 1;
			return true;
		}
		catch (IOException ioe)
		{
			log.warn("Error writing loot data to file {}: {}", randomEventsFile.getFileName(), ioe.getMessage());
			return false;
		}
	}

}
