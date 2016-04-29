package snmoedct2emx.convertor.io;

import static java.util.Collections.emptySet;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.molgenis.data.Entity;
import org.molgenis.data.csv.CsvRepository;

import snmoedct2emx.convertor.beans.SnomedCTDescription;

public class SnomedCTConceptDescriptioinRepo implements Closeable
{
	private final static String ID = "id";
	private final static String ACTIVE = "active";
	private final static String CONCEPT_ID = "conceptId";
	private final static String TYPE_ID = "typeId";
	private final static String TERM = "term";

	private final CsvRepository csvRepository;
	private final Map<String, Set<SnomedCTDescription>> cachedConceptSynonymMap = new HashMap<String, Set<SnomedCTDescription>>();

	public SnomedCTConceptDescriptioinRepo(File file)
	{
		csvRepository = new CsvRepository(file, null, '\t');
		loadFromRepository();
	}

	public SnomedCTDescription getLabel(String conceptId)
	{
		if (cachedConceptSynonymMap.containsKey(conceptId))
		{
			for (SnomedCTDescription snomedCTDescription : cachedConceptSynonymMap.get(conceptId))
			{
				if (snomedCTDescription.isLabel()) return snomedCTDescription;
			}
		}
		return null;
	}

	public Set<SnomedCTDescription> getSynonyms(String conceptId)
	{
		return cachedConceptSynonymMap.containsKey(conceptId) ? cachedConceptSynonymMap.get(conceptId) : emptySet();
	}

	private void loadFromRepository()
	{
		System.out.println("INFO:Starting to load information from SnomedCT description file");

		int count = 0;

		for (Entity entity : csvRepository)
		{
			if (!cachedConceptSynonymMap.containsKey(entity.getString(CONCEPT_ID)))
			{
				cachedConceptSynonymMap.put(entity.getString(CONCEPT_ID), new HashSet<SnomedCTDescription>());
			}
			cachedConceptSynonymMap.get(entity.getString(CONCEPT_ID))
					.add(new SnomedCTDescription(entity.getString(ID), entity.getString(CONCEPT_ID),
							entity.getInt(ACTIVE) == 1, entity.getString(TYPE_ID), entity.getString(TERM)));
			count++;
			if (count % 5000 == 0)
			{
				System.out.println("INFO:" + count + " rows in description file have been loaded...");
			}
		}

		System.out.println("INFO:SnomedCT description file with " + count + " of rows has been loaded...");
		System.out.println();
	}

	public Iterator<SnomedCTDescription> iterator()
	{
		final Iterator<Entity> iterator = csvRepository.iterator();

		return new Iterator<SnomedCTDescription>()
		{
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			public SnomedCTDescription next()
			{
				Entity entity = iterator.next();

				return new SnomedCTDescription(entity.getString(ID), entity.getString(CONCEPT_ID),
						entity.getInt(ACTIVE) == 1, entity.getString(TYPE_ID), entity.getString(TERM));
			}

			public void remove()
			{
			}

			public void forEachRemaining(Consumer<? super SnomedCTDescription> action)
			{
			}
		};
	}

	public void close() throws IOException
	{
		csvRepository.close();
	}
}