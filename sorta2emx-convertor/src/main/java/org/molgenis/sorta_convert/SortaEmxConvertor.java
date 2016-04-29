package org.molgenis.sorta_convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.MolgenisInvalidFormatException;
import org.molgenis.data.Repository;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.excel.ExcelRepository;
import org.molgenis.data.excel.ExcelRepositoryCollection;
import org.molgenis.data.processor.CellProcessor;
import org.molgenis.data.processor.TrimProcessor;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.UuidGenerator;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.util.ZipUtils;
import org.molgenis.util.ZipUtils.DirectoryStructure;

import com.google.common.collect.Lists;

public class SortaEmxConvertor
{
	private final static Character DEFAULT_SEPARATOR = ',';
	private final static String CONCEPT_ID_FIELD = "Concept ID";
	private final static String CONCEPT_LABEL_FIELD = "Concept Label";
	private final static String SYSTEM_ID_FIELD = "System ID";
	private final static IdGenerator ID_GENERATOR = new UuidGenerator();

	public void convertCsvFile(File inputCsvFile, File outputFile) throws IOException
	{
		CsvRepository csvRepository = new CsvRepository(inputCsvFile,
				Arrays.<CellProcessor> asList(new TrimProcessor()), DEFAULT_SEPARATOR);
		convert(csvRepository, outputFile);
		csvRepository.close();
	}

	public void convertExcelFile(File inputExcelFile, File outputFile)
			throws IOException, MolgenisInvalidFormatException
	{
		ExcelRepositoryCollection collection = new ExcelRepositoryCollection(inputExcelFile);
		ExcelRepository repository = collection.getSheet(0);
		convert(repository, outputFile);
		repository.close();
	}

	public void convert(Repository repository, File outputFile) throws IOException
	{
		if (validateCsvRepoMetaData(repository))
		{
			File outputFolder = outputFile.getParentFile();

			Map<String, List<String>> ontologyTermSynonymMrefs = new HashMap<>();
			Map<String, String> codingSystems = new HashMap<>();
			Set<String> uniqueOntologyTermIds = new HashSet<>();

			Iterator<Entity> iterator = repository.iterator();
			int index = 2;
			while (iterator.hasNext())
			{
				Entity next = iterator.next();
				String conceptId = next.getString(CONCEPT_ID_FIELD);
				if (StringUtils.isBlank(conceptId))
				{
					System.out.println("The Concept Id at row " + index + " is empty, see details " + next.toString()
							+ ". Please fix the Concept Id for this row and import again!");
					System.exit(0);
				}
				String conceptLabel = next.getString(CONCEPT_LABEL_FIELD);
				if (StringUtils.isBlank(conceptLabel))
				{
					System.out.println("The Concept Label at row " + index + " is empty, see details " + next.toString()
							+ ". Please fix the Concept Label for this row and import again!");
					System.exit(0);
				}
				String codeSystemId = next.getString(SYSTEM_ID_FIELD);
				if (StringUtils.isBlank(codeSystemId))
				{
					System.out.println("The Code System ID at row " + index + " is empty, see details "
							+ next.toString() + ". Please fix the Code System ID for this row and import again!");
					System.exit(0);
				}
				index++;
			}

			// Convert synonyms
			File ontologyTermSynonymFile = new File(
					outputFolder.getAbsolutePath() + File.separator + OntologyTermSynonymMetaData.ENTITY_NAME + ".csv");
			CsvWriter csvWriterOntologyTermSynonym = new CsvWriter(ontologyTermSynonymFile);
			csvWriterOntologyTermSynonym.writeAttributes(OntologyTermSynonymMetaData.INSTANCE.getAtomicAttributes());

			iterator = repository.iterator();
			while (iterator.hasNext())
			{
				Entity next = iterator.next();
				String conceptId = next.getString(CONCEPT_ID_FIELD);
				Entity createOntologyTermSynonymEntity = createOntologyTermSynonymEntity(next);
				if (!ontologyTermSynonymMrefs.containsKey(conceptId))
				{
					ontologyTermSynonymMrefs.put(conceptId, new ArrayList<>());
				}
				ontologyTermSynonymMrefs.get(conceptId)
						.add(createOntologyTermSynonymEntity.getString(OntologyTermSynonymMetaData.ID));

				csvWriterOntologyTermSynonym.add(createOntologyTermSynonymEntity);
			}
			csvWriterOntologyTermSynonym.close();

			// Convert coding systems
			File ontologyFile = new File(
					outputFolder.getAbsolutePath() + File.separator + OntologyMetaData.ENTITY_NAME + ".csv");
			CsvWriter csvWriterOntology = new CsvWriter(ontologyFile);
			csvWriterOntology.writeAttributes(OntologyMetaData.INSTANCE.getAtomicAttributes());

			iterator = repository.iterator();
			while (iterator.hasNext())
			{
				Entity next = iterator.next();
				String codingSystem = next.getString(SYSTEM_ID_FIELD);
				if (!codingSystems.containsKey(codingSystem))
				{
					Entity createOntologyEntity = createOntologyEntity(next);
					csvWriterOntology.add(createOntologyEntity);

					String ontologyName = createOntologyEntity.getString(OntologyMetaData.ONTOLOGY_NAME);
					String ontologyId = createOntologyEntity.getString(OntologyMetaData.ID);
					codingSystems.put(ontologyName, ontologyId);
				}
			}
			csvWriterOntology.close();

			// Convert concepts
			File ontologyTermFile = new File(
					outputFolder.getAbsolutePath() + File.separator + OntologyTermMetaData.ENTITY_NAME + ".csv");
			CsvWriter csvWriterOntologyTerm = new CsvWriter(ontologyTermFile);
			csvWriterOntologyTerm.writeAttributes(OntologyTermMetaData.INSTANCE.getAtomicAttributes());
			iterator = repository.iterator();
			while (iterator.hasNext())
			{
				Entity next = iterator.next();
				Entity createOntologyTermEntity = createOntologyTermEntity(next, codingSystems,
						ontologyTermSynonymMrefs);
				String ontologyTermId = createOntologyTermEntity.getString(OntologyTermMetaData.ID);
				if (!uniqueOntologyTermIds.contains(ontologyTermId))
				{
					csvWriterOntologyTerm.add(createOntologyTermEntity);
					uniqueOntologyTermIds.add(ontologyTermId);
				}
			}

			csvWriterOntologyTerm.close();

			ZipUtils.compress(Arrays.asList(ontologyFile, ontologyTermFile, ontologyTermSynonymFile), outputFile,
					DirectoryStructure.EXCLUDE_DIR);

			FileUtils.deleteQuietly(ontologyFile);
			FileUtils.deleteQuietly(ontologyTermFile);
			FileUtils.deleteQuietly(ontologyTermSynonymFile);
		}
		else
		{
			System.out.println("Your input file does not have the correct column headers : "
					+ Arrays.asList(CONCEPT_ID_FIELD, CONCEPT_LABEL_FIELD, SYSTEM_ID_FIELD));
		}
	}

	Entity createOntologyTermEntity(Entity entity, Map<String, String> codingSystems,
			Map<String, List<String>> ontologyTermSynonymMrefs)
	{
		String systemId = entity.getString(SYSTEM_ID_FIELD);
		String conceptId = entity.getString(CONCEPT_ID_FIELD);
		String conceptName = entity.getString(CONCEPT_LABEL_FIELD);

		MapEntity mapEntity = new MapEntity();
		mapEntity.set(OntologyTermMetaData.ID, conceptId);
		mapEntity.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, conceptId);
		mapEntity.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, conceptName);
		mapEntity.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, ontologyTermSynonymMrefs.get(conceptId));
		mapEntity.set(OntologyTermMetaData.ONTOLOGY, codingSystems.get(systemId));
		return mapEntity;
	}

	Entity createOntologyEntity(Entity entity)
	{
		String systemId = entity.getString(SYSTEM_ID_FIELD);
		MapEntity mapEntity = new MapEntity();
		mapEntity.set(OntologyMetaData.ID, systemId.replaceAll("[^a-zA-Z0-9]", "_"));
		mapEntity.set(OntologyMetaData.ONTOLOGY_NAME, systemId);
		mapEntity.set(OntologyMetaData.ONTOLOGY_IRI, systemId);
		return mapEntity;
	}

	Entity createOntologyTermSynonymEntity(Entity entity)
	{
		String conceptLabel = entity.getString(CONCEPT_LABEL_FIELD);
		String ontologyTermSynonymId = ID_GENERATOR.generateId();
		MapEntity mapEntity = new MapEntity();
		mapEntity.set(OntologyTermSynonymMetaData.ID, ontologyTermSynonymId);
		mapEntity.set(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM, conceptLabel);
		return mapEntity;
	}

	boolean validateCsvRepoMetaData(Repository repository)
	{
		EntityMetaData entityMetaData = repository.getEntityMetaData();
		return StreamSupport.stream(entityMetaData.getAtomicAttributes().spliterator(), false)
				.map(AttributeMetaData::getName).collect(Collectors.toList())
				.containsAll(Lists.newArrayList(CONCEPT_ID_FIELD, CONCEPT_LABEL_FIELD, SYSTEM_ID_FIELD));
	}
}