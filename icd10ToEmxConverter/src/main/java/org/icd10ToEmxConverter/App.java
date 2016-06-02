package org.icd10ToEmxConverter;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.molgenis.data.Entity;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.UuidGenerator;

/**
 * Hello world!
 *
 */
public class App
{
	private final static UuidGenerator UUID_GENERATOR = new UuidGenerator();

	public static void main(String[] args) throws IOException
	{
		if (args.length == 2)
		{
			File inputFile = new File(args[0]);
			File folderPath = new File(args[1]);

			if (inputFile.exists() && folderPath.exists())
			{
				CsvWriter ontologyCsvWriter = new CsvWriter(
						new File(getOutPutFilePath(folderPath.getAbsolutePath(), "Ontology_Ontology.csv")));
				ontologyCsvWriter.writeAttributeNames(Arrays.asList("id", "ontologyIRI", "ontologyName"));

				MapEntity icd10OntologyEntity = new MapEntity();
				icd10OntologyEntity.set("id", "ICD10");
				icd10OntologyEntity.set("ontologyName", "ICD 10");
				icd10OntologyEntity.set("ontologyIRI", "ICD10");
				ontologyCsvWriter.add(icd10OntologyEntity);
				ontologyCsvWriter.close();

				CsvWriter ontologyTermCsvWriter = new CsvWriter(
						new File(getOutPutFilePath(folderPath.getAbsolutePath(), "Ontology_OntologyTerm.csv")));
				ontologyTermCsvWriter.writeAttributeNames(Arrays.asList("id", "ontologyTermIRI", "ontologyTermName",
						"ontologyTermSynonym", "ontologyTermDynamicAnnotation", "nodePath", "ontology"));

				CsvWriter ontologyTermSynonymCsvWriter = new CsvWriter(
						new File(getOutPutFilePath(folderPath.getAbsolutePath(), "Ontology_OntologyTermSynonym.csv")));
				ontologyTermSynonymCsvWriter.writeAttributeNames(Arrays.asList("id", "ontologyTermSynonym"));

				CsvRepository csvRepository = new CsvRepository(inputFile, Collections.emptyList(), ',');

				for (Entity entity : csvRepository)
				{
					String code = entity.getString("Code");
					String name = entity.getString("Name");

					Entity synonymEntity = createSynonymEntity(name);

					Entity ontologyTermEntity = createOntologyTermEntity(code, name,
							asList(synonymEntity.getString("id")));

					ontologyTermCsvWriter.add(ontologyTermEntity);

					ontologyTermSynonymCsvWriter.add(synonymEntity);

				}

				csvRepository.close();
				ontologyTermCsvWriter.close();
				ontologyTermSynonymCsvWriter.close();
			}
		}
	}

	private static Entity createOntologyTermEntity(String code, String name, List<String> synonymIds)
	{
		MapEntity ontologyTermEntity = new MapEntity();
		ontologyTermEntity.set("id", code);
		ontologyTermEntity.set("ontology", "ICD10");
		ontologyTermEntity.set("ontologyTermIRI", code);
		ontologyTermEntity.set("ontologyTermName", name);
		ontologyTermEntity.set("ontologyTermSynonym", synonymIds);
		return ontologyTermEntity;
	}

	private static Entity createSynonymEntity(String synonym)
	{
		MapEntity synonymEntity = new MapEntity();
		synonymEntity.set("id", UUID_GENERATOR.generateId());
		synonymEntity.set("ontologyTermSynonym", synonym);
		return synonymEntity;
	}

	private static String getOutPutFilePath(String folderPath, String fileName)
	{
		StringBuilder sb = new StringBuilder();
		if (folderPath.charAt(folderPath.length() - 1) == File.separatorChar)
		{
			sb.append(folderPath).append(fileName);
		}
		else
		{
			sb.append(folderPath).append(File.separatorChar).append(fileName);
		}
		return sb.toString();
	}
}
