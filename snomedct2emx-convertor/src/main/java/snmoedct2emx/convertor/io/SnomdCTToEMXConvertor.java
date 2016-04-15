package snmoedct2emx.convertor.io;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.UuidGenerator;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import snmoedct2emx.convertor.beans.IdentifiableNodePath;
import snmoedct2emx.convertor.beans.SnomedCTConcept;
import snmoedct2emx.convertor.beans.SnomedCTDescription;

public class SnomdCTToEMXConvertor
{
	private static final String SNOMED_CT_URL_PREFIX = "http://purl.bioontology.org/ontology/SNOMEDCT/";

	public static void main(String args[]) throws IOException, ParseException
	{
		Options options = createOptions();

		CommandLineParser parser = new BasicParser();

		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("concept") && cmd.hasOption("conceptDescription") && cmd.hasOption("conceptRelation")
				&& cmd.hasOption("output"))
		{
			String conceptFilePath = cmd.getOptionValue("concept");

			String conceptDescriptionFilePath = cmd.getOptionValue("conceptDescription");

			String conceptRelationFilePath = cmd.getOptionValue("conceptRelation");

			String folderPath = cmd.getOptionValue("output");

			SnomedCTConceptRepo snomedCTConceptRepo = new SnomedCTConceptRepo(new File(conceptFilePath));

			SnomedCTConceptDescriptioinRepo snomedCTConceptDescriptioinRepo = new SnomedCTConceptDescriptioinRepo(
					new File(conceptDescriptionFilePath));

			SnomeCTConceptRelationRepo snomeCTConceptRelationRepo = new SnomeCTConceptRelationRepo(new File(
					conceptRelationFilePath));

			String ontologyId = generateRandomId();

			createSnomedCTOntology(folderPath, ontologyId);

			convertConcepts(folderPath, snomedCTConceptRepo, snomedCTConceptDescriptioinRepo,
					snomeCTConceptRelationRepo, ontologyId);

			snomedCTConceptRepo.close();

			snomedCTConceptDescriptioinRepo.close();

			snomeCTConceptRelationRepo.close();

		}
		else
		{
			showHelpMessage(options);
		}
	}

	private static void createSnomedCTOntology(String folderPath, String ontologyId) throws IOException
	{
		CsvWriter ontologyTermCsvWriter = new CsvWriter(
				new File(getOutPutFilePath(folderPath, "Ontology_Ontology.csv")));

		ontologyTermCsvWriter.writeAttributeNames(Arrays.asList("id", "ontologyIRI", "ontologyName"));
		MapEntity entity = new MapEntity();
		entity.set("id", ontologyId);
		entity.set("ontologyName", "Snomed CT");
		entity.set("ontologyIRI", SNOMED_CT_URL_PREFIX);
		ontologyTermCsvWriter.add(entity);
		ontologyTermCsvWriter.close();
	}

	public static void convertConcepts(String folderPath, SnomedCTConceptRepo snomedCTConceptRepo,
			SnomedCTConceptDescriptioinRepo snomedCTConceptDescriptioinRepo,
			SnomeCTConceptRelationRepo snomeCTConceptRelationRepo, String ontologyId) throws IOException
	{
		// Create CsvWriter for writing OntologyTerm entities
		CsvWriter ontologyTermCsvWriter = new CsvWriter(new File(getOutPutFilePath(folderPath,
				"Ontology_OntologyTerm.csv")));
		ontologyTermCsvWriter.writeAttributeNames(Arrays.asList("id", "ontologyTermIRI", "ontologyTermName",
				"ontologyTermSynonym", "ontologyTermDynamicAnnotation", "nodePath", "ontology"));

		// Create CsvWriter for writing OntologyTermSynonym entities
		CsvWriter ontologyTermSynonymCsvWriter = new CsvWriter(new File(getOutPutFilePath(folderPath,
				"Ontology_OntologyTermSynonym.csv")));
		ontologyTermSynonymCsvWriter.writeAttributeNames(Arrays.asList("id", "ontologyTermSynonym"));

		// Create CsvWriter for writing OntologyNodePath entities
		CsvWriter ontologyTermNodePathCsvWriter = new CsvWriter(new File(getOutPutFilePath(folderPath,
				"Ontology_OntologyTermNodePath.csv")));
		ontologyTermNodePathCsvWriter.writeAttributeNames(Arrays.asList("id", "root", "nodePath"));

		System.out.println("INFO:Starting to convert SnomeCT Concept file to EMX Ontology_OntologyTerm table...");

		int count = 0;
		for (SnomedCTConcept snomedCTConcept : snomedCTConceptRepo.getAllConcepts())
		{
			SnomedCTDescription snomedCTConceptLabel = snomedCTConceptDescriptioinRepo
					.getLabel(snomedCTConcept.getId());

			if (snomedCTConcept.isActive() && snomedCTConceptLabel != null)
			{
				createSnomedConceptEntity(snomedCTConceptDescriptioinRepo, snomeCTConceptRelationRepo, ontologyId,
						ontologyTermCsvWriter, snomedCTConcept, snomedCTConceptLabel);

				createSnomedSynonymEntity(snomedCTConceptDescriptioinRepo, ontologyTermSynonymCsvWriter,
						snomedCTConcept);

				createSnomedNodePathEntity(snomeCTConceptRelationRepo, ontologyTermNodePathCsvWriter, snomedCTConcept);
			}

			count++;
			if (count % 5000 == 0)
			{
				System.out.println("INFO:" + count + " of rows in SnomedCT concepts have been converted...");
			}

		}
		ontologyTermCsvWriter.close();

		ontologyTermSynonymCsvWriter.close();

		ontologyTermNodePathCsvWriter.close();

		System.out.println("INFO:SnomedCT concept file with " + count + " of rows has been converted to EMX csv file");
		System.out.println();
	}

	private static void createSnomedNodePathEntity(SnomeCTConceptRelationRepo snomeCTConceptRelationRepo,
			CsvWriter ontologyTermNodePathCsvWriter, SnomedCTConcept snomedCTConcept)
	{
		for (IdentifiableNodePath identifiableNodePath : snomeCTConceptRelationRepo.getNodePathObject(snomedCTConcept
				.getId()))
		{
			MapEntity mapEntity = new MapEntity();
			mapEntity.set("id", identifiableNodePath.getId());
			mapEntity.set("root", identifiableNodePath.isRoot());
			mapEntity.set("nodePath", identifiableNodePath.getNodePath());
			ontologyTermNodePathCsvWriter.add(mapEntity);
		}
	}

	public static void createSnomedSynonymEntity(SnomedCTConceptDescriptioinRepo snomeCTConceptDescriptionRepo,
			CsvWriter ontologyTermSynonymCsvWriter, SnomedCTConcept snomedCTConcept)
	{
		for (SnomedCTDescription snomedCTDescription : snomeCTConceptDescriptionRepo.getSynonyms(snomedCTConcept
				.getId()))
		{
			MapEntity mapEntity = new MapEntity();
			mapEntity.set("id", snomedCTDescription.getId());
			mapEntity.set("ontologyTermSynonym", snomedCTDescription.getTerm());
			ontologyTermSynonymCsvWriter.add(mapEntity);
		}
	}

	public static void createSnomedConceptEntity(SnomedCTConceptDescriptioinRepo snomedCTConceptDescriptioinRepo,
			SnomeCTConceptRelationRepo snomeCTConceptRelationRepo, String ontologyId, CsvWriter ontologyTermCsvWriter,
			SnomedCTConcept snomedCTConcept, SnomedCTDescription snomedCTConceptLabel)
	{
		MapEntity entity = new MapEntity();
		entity.set("id", snomedCTConcept.getId());
		entity.set("ontologyTermIRI", createSnomedCTOntologyTermUrl(snomedCTConcept));
		entity.set("ontology", ontologyId);
		entity.set("ontologyTermName", snomedCTConceptLabel.getTerm());
		ImmutableList<String> synonymIds = FluentIterable
				.from(snomedCTConceptDescriptioinRepo.getSynonyms(snomedCTConcept.getId()))
				.filter(new Predicate<SnomedCTDescription>()
				{
					public boolean apply(SnomedCTDescription snomedCTDescription)
					{
						return !snomedCTDescription.isDefinition() && snomedCTDescription.isActive();
					}
				}).transform(new Function<SnomedCTDescription, String>()
				{
					public String apply(SnomedCTDescription snomedCTDescription)
					{
						return snomedCTDescription.getId();
					}
				}).toList();
		entity.set("ontologyTermSynonym", StringUtils.join(synonymIds, ','));

		ImmutableList<String> nodePathIds = FluentIterable
				.from(snomeCTConceptRelationRepo.getNodePathObject(snomedCTConcept.getId()))
				.transform(new Function<IdentifiableNodePath, String>()
				{
					public String apply(IdentifiableNodePath identifiableNodePath)
					{
						return identifiableNodePath.getId();
					}
				}).toList();

		entity.set("nodePath", StringUtils.join(nodePathIds, ','));

		ontologyTermCsvWriter.add(entity);
	}

	public static String createSnomedCTOntologyTermUrl(SnomedCTConcept snomedCTConcept)
	{
		return SNOMED_CT_URL_PREFIX + snomedCTConcept.getId();
	}

	private static String getOutPutFilePath(String folderPath, String fileName)
	{
		StringBuilder sb = new StringBuilder();
		if (folderPath.charAt(folderPath.length() - 1) == '/')
		{
			sb.append(folderPath).append(fileName);
		}
		else
		{
			sb.append(folderPath).append('/').append(fileName);
		}
		return sb.toString();
	}

	public static String generateRandomId()
	{
		IdGenerator idGenerator = new UuidGenerator();
		return idGenerator.generateId();
	}

	private static void showHelpMessage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp("java -jar snomedctToEmx.jar", "where options include:", options,
						"\nTo convert snomed ct, it is suggested to increase the maximum amount of memory allocated to java e.g. -Xmx2G");
	}

	private static Options createOptions()
	{
		Options options = new Options();
		options.addOption(new Option("concept", true, "provide the snomed ct concept file"));
		options.addOption(new Option("conceptDescription", true, "provide the snomed ct concept description file"));
		options.addOption(new Option("conceptRelation", true, "provide the snomed ct concept relation file"));
		options.addOption(new Option("output", true, "provide the path of output folder"));
		return options;
	}
}
