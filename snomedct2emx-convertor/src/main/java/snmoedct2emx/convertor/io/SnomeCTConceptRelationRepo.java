package snmoedct2emx.convertor.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.Entity;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.support.UuidGenerator;

import snmoedct2emx.convertor.beans.IdentifiableNodePath;
import snmoedct2emx.convertor.beans.SnomedCTSubClassRelation;

public class SnomeCTConceptRelationRepo implements Closeable
{
	private final CsvRepository csvRepository;
	private final Map<String, Set<IdentifiableNodePath>> nodePathMapping = new HashMap<String, Set<IdentifiableNodePath>>();
	private final static UuidGenerator uuidGenerator = new UuidGenerator();

	public static enum TypeEnum
	{
		IS_A("116680003");

		private String label;

		TypeEnum(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	};

	public SnomeCTConceptRelationRepo(File file)
	{
		csvRepository = new CsvRepository(file, null, '\t');
		Map<String, Set<String>> relationMapping = loadFromRepository();
		System.out.println("Starting to compute the nodePath...");
		recursivelyCreateNodePath(new AtomicInteger(0), "", findTopNodes(relationMapping), true, relationMapping,
				nodePathMapping);
		System.out.println("Finished to compute the nodePath...");
	}

	public static void recursivelyCreateNodePath(AtomicInteger counter, String parentNodePath, Set<String> conceptIds,
			boolean isRoot, Map<String, Set<String>> relationMapping,
			Map<String, Set<IdentifiableNodePath>> nodePathMapping)
	{
		int index = 0;
		for (String conceptId : conceptIds)
		{
			String nodePath = constructNodePath(parentNodePath, index);
			if (relationMapping.containsKey(conceptId))
			{
				recursivelyCreateNodePath(counter, nodePath, relationMapping.get(conceptId), false, relationMapping,
						nodePathMapping);
			}
			if (!nodePathMapping.containsKey(conceptId))
			{
				nodePathMapping.put(conceptId, new LinkedHashSet<IdentifiableNodePath>());
			}
			nodePathMapping.get(conceptId).add(new IdentifiableNodePath(uuidGenerator.generateId(), isRoot, nodePath));
			index++;
			if (counter.incrementAndGet() % 5000 == 0)
			{
				System.out.println("INFO:" + counter.get() + " is_a relations have been loaded...");
			}

		}
	}

	public static Set<String> findTopNodes(Map<String, Set<String>> relationMapping)
	{
		Set<String> allChildren = new HashSet<String>();
		for (Set<String> setOfChilren : relationMapping.values())
		{
			allChildren.addAll(setOfChilren);
		}
		Set<String> topParents = new LinkedHashSet<String>();

		for (String parent : relationMapping.keySet())
		{
			if (!allChildren.contains(parent))
			{
				topParents.add(parent);
			}
		}
		return topParents;
	}

	private Map<String, Set<String>> loadFromRepository()
	{
		Map<String, Set<String>> relationMapping = new HashMap<String, Set<String>>();

		System.out.println("INFO:Starting to load information from SnomedCT Relation file");

		int count = 0;
		for (SnomedCTSubClassRelation snomedCTSubClassRelation : getAllSubClassRelations())
		{
			if (snomedCTSubClassRelation.isActive())
			{
				String child = snomedCTSubClassRelation.getSoureceId();
				String parent = snomedCTSubClassRelation.getDestinationId();

				if (!relationMapping.containsKey(parent))
				{
					relationMapping.put(parent, new HashSet<String>());
				}
				relationMapping.get(parent).add(child);
			}

			count++;
			if (count % 5000 == 0)
			{
				System.out.println("INFO:" + count + " rows in Relation file have been loaded...");
			}
		}

		System.out.println("INFO:SnomedCT Relation file with " + count + " of rows has been loaded");
		System.out.println();

		return relationMapping;
	}

	private Iterable<SnomedCTSubClassRelation> getAllSubClassRelations()
	{
		Map<String, SnomedCTSubClassRelation> relations = new HashMap<String, SnomedCTSubClassRelation>();
		for (Entity entity : csvRepository)
		{
			if (entity.getString("typeId").equals(TypeEnum.IS_A.toString()))
			{
				String id = entity.getString("id");
				boolean active = entity.getInt("active") == 1;
				String sourceId = entity.getString("sourceId");
				String destinationId = entity.getString("destinationId");
				relations.put(id, new SnomedCTSubClassRelation(id, active, sourceId, destinationId));
			}
		}
		return relations.values();
	}

	private static String constructNodePath(String parentNodePath, int currentPosition)
	{
		StringBuilder nodePathStringBuilder = new StringBuilder();
		if (!StringUtils.isEmpty(parentNodePath)) nodePathStringBuilder.append(parentNodePath).append('.');
		nodePathStringBuilder.append(currentPosition).append('[')
				.append(nodePathStringBuilder.toString().split("\\.").length - 1).append(']');
		return nodePathStringBuilder.toString();
	}

	public void close() throws IOException
	{
		csvRepository.close();
	}

	public Set<IdentifiableNodePath> getNodePathObject(String conceptId)
	{
		return nodePathMapping.containsKey(conceptId) ? nodePathMapping.get(conceptId) : Collections
				.<IdentifiableNodePath> emptySet();
	}
}