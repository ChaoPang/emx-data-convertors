package snmoedct2emx.convertor.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.molgenis.data.Entity;
import org.molgenis.data.csv.CsvRepository;

import snmoedct2emx.convertor.beans.SnomedCTConcept;

public class SnomedCTConceptRepo implements Closeable
{
	private final static String ID = "id";
	private final static String ACTIVE = "active";

	private final CsvRepository csvRepository;

	public SnomedCTConceptRepo(File file)
	{
		csvRepository = new CsvRepository(file, null, '\t');
	}

	public Iterable<SnomedCTConcept> getAllConcepts()
	{
		Map<String, SnomedCTConcept> snomedCTConcepts = new HashMap<String, SnomedCTConcept>();

		for (Entity entity : csvRepository)
		{
			String id = entity.getString(ID);
			boolean active = entity.getInt(ACTIVE) == 1;
			snomedCTConcepts.put(id, new SnomedCTConcept(id, active));
		}
		return snomedCTConcepts.values();
	}

	public void close() throws IOException
	{
		csvRepository.close();
	}
}
