/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Test reading of git config
 */
public class RepositoryConfigTest extends RepositoryTestCase {
	/**
	 * Read config item with no value from a section without a subsection.
	 *
	 * @throws IOException
	 */
	public void test001_ReadBareKey() throws IOException {
		final File path = writeTrashFile("config_001", "[foo]\nbar\n");
		RepositoryConfig repositoryConfig = new RepositoryConfig(null, path);
		System.out.println(repositoryConfig.getString("foo", null, "bar"));
		assertEquals(true, repositoryConfig.getBoolean("foo", null, "bar", false));
		assertEquals("", repositoryConfig.getString("foo", null, "bar"));
	}

	/**
	 * Read various data from a subsection.
	 *
	 * @throws IOException
	 */
	public void test002_ReadWithSubsection() throws IOException {
		final File path = writeTrashFile("config_002", "[foo \"zip\"]\nbar\n[foo \"zap\"]\nbar=false\nn=3\n");
		RepositoryConfig repositoryConfig = new RepositoryConfig(null, path);
		assertEquals(true, repositoryConfig.getBoolean("foo", "zip", "bar", false));
		assertEquals("", repositoryConfig.getString("foo","zip", "bar"));
		assertEquals(false, repositoryConfig.getBoolean("foo", "zap", "bar", true));
		assertEquals("false", repositoryConfig.getString("foo", "zap", "bar"));
		assertEquals(3, repositoryConfig.getInt("foo", "zap", "n", 4));
		assertEquals(4, repositoryConfig.getInt("foo", "zap","m", 4));
	}

	public void test003_PutRemote() throws IOException {
		File cfgFile = writeTrashFile("config_003", "");
		RepositoryConfig repositoryConfig = new RepositoryConfig(null, cfgFile);
		repositoryConfig.setString("sec", "ext", "name", "value");
		repositoryConfig.setString("sec", "ext", "name2", "value2");
		repositoryConfig.save();
		checkFile(cfgFile, "[sec \"ext\"]\n\tname = value\n\tname2 = value2\n");
	}

	public void test004_PutGetSimple() throws IOException {
		File cfgFile = writeTrashFile("config_004", "");
		RepositoryConfig repositoryConfig = new RepositoryConfig(null, cfgFile);
		repositoryConfig.setString("my", null, "somename", "false");
		repositoryConfig.save();
		checkFile(cfgFile, "[my]\n\tsomename = false\n");
		assertEquals("false", repositoryConfig
				.getString("my", null, "somename"));
	}

	public void test005_PutGetStringList() throws IOException {
		File cfgFile = writeTrashFile("config_005", "");
		RepositoryConfig repositoryConfig = new RepositoryConfig(null, cfgFile);
		final LinkedList<String> values = new LinkedList<String>();
		values.add("value1");
		values.add("value2");
		repositoryConfig.setStringList("my", null, "somename", values);
		repositoryConfig.save();
		assertTrue(Arrays.equals(values.toArray(), repositoryConfig
				.getStringList("my", null, "somename")));
		checkFile(cfgFile, "[my]\n\tsomename = value1\n\tsomename = value2\n");
	}
}
