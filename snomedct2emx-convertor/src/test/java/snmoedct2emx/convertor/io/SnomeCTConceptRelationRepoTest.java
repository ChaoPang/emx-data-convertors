package snmoedct2emx.convertor.io;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import snmoedct2emx.convertor.beans.IdentifiableNodePath;

public class SnomeCTConceptRelationRepoTest
{
	@Test
	public void findTopNodes()
	{
		Map<String, Set<String>> testMap = ImmutableMap.<String, Set<String>> of("1",
				ImmutableSet.<String> of("2", "3"), "2", ImmutableSet.<String> of("4", "5"), "3",
				ImmutableSet.<String> of("6", "5"), "7", ImmutableSet.<String> of("8", "9"));

		Set<String> findTopNodes = SnomeCTConceptRelationRepo.findTopNodes(testMap);

		assertEquals(findTopNodes.toString(), Sets.newHashSet("1", "7").toString());
	}

	@Test
	public void recursivelyCreateNodePath()
	{
		Map<String, Set<String>> testMap = ImmutableMap.<String, Set<String>> of("1",
				ImmutableSet.<String> of("2", "3"), "2", ImmutableSet.<String> of("4", "5"), "3",
				ImmutableSet.<String> of("6", "5"), "7", ImmutableSet.<String> of("8", "9"));

		Map<String, Set<IdentifiableNodePath>> recursivelyCreateNodePath = new HashMap<String, Set<IdentifiableNodePath>>();
		SnomeCTConceptRelationRepo.recursivelyCreateNodePath(new AtomicInteger(0), "", Sets.newHashSet("1", "7"), true,
				testMap, recursivelyCreateNodePath);

		Set<IdentifiableNodePath> topNodePathSet = recursivelyCreateNodePath.get("1");
		assertEquals(1, topNodePathSet.size());
		for (IdentifiableNodePath identifiableNodePath : topNodePathSet)
		{
			assertEquals(identifiableNodePath.getNodePath(), "0[0]");
		}

		Set<IdentifiableNodePath> numberFiveNodePathSet = recursivelyCreateNodePath.get("5");
		assertEquals(2, numberFiveNodePathSet.size());

		for (IdentifiableNodePath identifiableNodePath : numberFiveNodePathSet)
		{
			assertTrue(
					Sets.newHashSet("0[0].0[1].1[2]", "0[0].1[1].1[2]").contains(identifiableNodePath.getNodePath()));
		}
	}
}
