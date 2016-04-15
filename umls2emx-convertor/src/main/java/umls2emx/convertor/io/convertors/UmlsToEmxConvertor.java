package umls2emx.convertor.io.convertors;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.collect.Lists;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.UuidGenerator;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.ontology.core.model.Ontology;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import umls2emx.convertor.beans.UmlsAtom;
import umls2emx.convertor.beans.UmlsConcept;
import umls2emx.convertor.beans.UmlsHierachicalRelation;
import umls2emx.convertor.beans.UmlsRelation;
import umls2emx.convertor.io.iterators.UmlsConceptHierachyIterator;
import umls2emx.convertor.io.iterators.UmlsConceptIterator;
import umls2emx.convertor.io.iterators.UmlsConceptRelationIterator;

public class UmlsToEmxConvertor implements UmlsConvertor
{
	private final static Log LOG = LogFactory.getLog(UmlsToEmxConvertor.class);

	private final static String MREF_SEPARATOR = ",";
	private final static String NODEPATH_SEPARATOR = "\\.";
	private final static String FILE_EXTENSION = ".csv";
	private final static String UMLS_ONTOLOGY_NAME = "UMLS";

	private final ListMultimap<String, String> relationLookUpTable;
	private final ListMultimap<String, String> linkedMapForConceptToHierachy;
	private final Ontology ontology;

	public UmlsToEmxConvertor()
	{
		UuidGenerator idGenenrator = new UuidGenerator();
		String generateId = idGenenrator.generateId();
		relationLookUpTable = ArrayListMultimap.create();
		linkedMapForConceptToHierachy = ArrayListMultimap.create();
		ontology = Ontology.create(generateId, UMLS_ONTOLOGY_NAME, UMLS_ONTOLOGY_NAME);
	}

	@Override
	public void convert(File umlsOutputFolder, File umlsConceptRelationFile, File umlsConceptHierachyFile,
			File umlsConceptFile, boolean includeAnnotation)
	{
		try
		{
			createUmlsOntology(umlsOutputFolder);

			if (includeAnnotation)
			{
				convertUmlsRelationToOntologyAnnotation(umlsConceptRelationFile, umlsOutputFolder);
			}

			convertHierachyToOntologyNodePath(umlsConceptHierachyFile, umlsOutputFolder);

			convertUmlsConceptsToOntologyTerms(umlsConceptFile, umlsOutputFolder);

		}
		catch (IOException e)
		{
			LOG.error(e.getMessage());
		}
	}

	private void createUmlsOntology(File outputFolder) throws IOException
	{
		CsvWriter ontologyCsvWriter = createCsvWriter(outputFolder, OntologyMetaData.INSTANCE);
		MapEntity entity = new MapEntity();
		entity.set(OntologyMetaData.ID, ontology.getId());
		entity.set(OntologyMetaData.ONTOLOGY_NAME, ontology.getName());
		entity.set(OntologyMetaData.ONTOLOGY_IRI, ontology.getIRI());
		ontologyCsvWriter.add(entity);
	}

	private void convertUmlsRelationToOntologyAnnotation(File inputFile, File outputFolder) throws IOException
	{
		System.out.println("---> Starting to convert the UMLS relation file");
		CsvWriter ontologyTermDynamicAnnotationCsvWriter = createCsvWriter(outputFolder,
				OntologyTermDynamicAnnotationMetaData.INSTANCE);

		UmlsConceptRelationIterator umlsConceptRelationIterator = new UmlsConceptRelationIterator(inputFile);

		while (umlsConceptRelationIterator.hasNext())
		{
			UmlsRelation umlsConceptRelation = umlsConceptRelationIterator.next();
			ontologyTermDynamicAnnotationCsvWriter
					.add(convertUmlsConceptRelationToDynamicOntologyAnnotation(umlsConceptRelation));
			relationLookUpTable.put(umlsConceptRelation.getCuiId2(), umlsConceptRelation.getRelationIdentifier());
		}

		umlsConceptRelationIterator.close();
		ontologyTermDynamicAnnotationCsvWriter.close();
		System.out.println("---> Converted the UMLS relation file");
	}

	private void convertHierachyToOntologyNodePath(File inputFile, File outputFolder) throws IOException
	{
		System.out.println("---> Starting to convert the UMLS hierachy file");
		CsvWriter ontologyTermNodePathCsvWriter = createCsvWriter(outputFolder, OntologyTermNodePathMetaData.INSTANCE);

		UmlsConceptHierachyIterator umlsConceptHierachyIterator = new UmlsConceptHierachyIterator(inputFile);

		while (umlsConceptHierachyIterator.hasNext())
		{
			UmlsHierachicalRelation umlsHierachicalRelation = umlsConceptHierachyIterator.next();
			ontologyTermNodePathCsvWriter.add(umlsHierachicalRelation.getAtoms().stream()
					.map(this::convertUmlsHierachicalRelationToOntologyTermSynonymEntity).collect(Collectors.toList()));
			umlsHierachicalRelation.getAtoms().stream().forEach(umlsAtom -> linkedMapForConceptToHierachy
					.put(umlsHierachicalRelation.getCuiId(), umlsAtom.getAtomId()));
		}
		umlsConceptHierachyIterator.close();
		ontologyTermNodePathCsvWriter.close();
		System.out.println("---> Converted the UMLS hierachy file");
	}

	private void convertUmlsConceptsToOntologyTerms(File inputFile, File outputFolder) throws IOException
	{
		System.out.println("---> Starting to convert the UMLS Concept file");
		CsvWriter ontologyTermCsvWriter = createCsvWriter(outputFolder, OntologyTermMetaData.INSTANCE);

		CsvWriter ontologyTermSynonymCsvWriter = createCsvWriter(outputFolder, OntologyTermSynonymMetaData.INSTANCE);

		UmlsConceptIterator umlsConceptIterator = new UmlsConceptIterator(inputFile);

		while (umlsConceptIterator.hasNext())
		{
			UmlsConcept umlsConcept = umlsConceptIterator.next();
			ontologyTermCsvWriter.add(convertUmlsConceptToOntologyTermEntity(umlsConcept));
			ontologyTermSynonymCsvWriter.add(umlsConcept.getConceptAtoms().stream()
					.map(this::convertUmlsConceptToOntologyTermSynonymEntity).collect(Collectors.toList()));
		}

		umlsConceptIterator.close();
		ontologyTermCsvWriter.close();
		ontologyTermSynonymCsvWriter.close();
		System.out.println("---> Converted the UMLS Concept file");
	}

	private Entity convertUmlsConceptRelationToDynamicOntologyAnnotation(UmlsRelation umlsConceptRelation)
	{
		MapEntity entity = new MapEntity(OntologyTermDynamicAnnotationMetaData.INSTANCE);
		entity.set(OntologyTermDynamicAnnotationMetaData.ID, umlsConceptRelation.getRelationIdentifier());
		entity.set(OntologyTermDynamicAnnotationMetaData.NAME,
				umlsConceptRelation.getUmlsRelationAttribute().getRelationAttribute());
		entity.set(OntologyTermDynamicAnnotationMetaData.VALUE, umlsConceptRelation.getCuiId1());
		entity.set(OntologyTermDynamicAnnotationMetaData.LABEL, umlsConceptRelation.toLabel());
		return entity;
	}

	private Entity convertUmlsHierachicalRelationToOntologyTermSynonymEntity(UmlsAtom umlsAtom)
	{
		String atomId = umlsAtom.getAtomId();
		String hierachy = umlsAtom.getHierachy();
		MapEntity entity = new MapEntity(OntologyTermNodePathMetaData.INSTANCE);
		entity.set(OntologyTermNodePathMetaData.ID, atomId);
		entity.set(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, hierachy);
		entity.set(OntologyTermNodePathMetaData.ROOT, isRoot(hierachy));
		return entity;
	}

	private Entity convertUmlsConceptToOntologyTermEntity(UmlsConcept umlsConcept)
	{
		String cuiId = umlsConcept.getCuiId();
		String preferredName = umlsConcept.getPreferredName();
		List<String> atomIds = umlsConcept.getConceptAtoms().stream().map(UmlsAtom::getAtomId)
				.collect(Collectors.toList());
		MapEntity entity = new MapEntity(OntologyTermMetaData.INSTANCE);
		entity.set(OntologyTermMetaData.ID, cuiId);
		entity.set(OntologyTermMetaData.ONTOLOGY, ontology.getId());
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, preferredName);
		// TODO: replace the cuiId with a proper url if it's available
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, cuiId);

		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH,
				concatenateListOfIds(linkedMapForConceptToHierachy.containsKey(cuiId)
						? linkedMapForConceptToHierachy.get(cuiId) : Collections.emptyList()));
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, concatenateListOfIds(atomIds));
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_DYNAMIC_ANNOTATION, concatenateListOfIds(
				relationLookUpTable.containsKey(cuiId) ? relationLookUpTable.get(cuiId) : Collections.emptyList()));
		return entity;
	}

	private Entity convertUmlsConceptToOntologyTermSynonymEntity(UmlsAtom umlsAtom)
	{
		String atomId = umlsAtom.getAtomId();
		String atomName = umlsAtom.getAtomName();
		MapEntity entity = new MapEntity(OntologyTermSynonymMetaData.INSTANCE);
		entity.set(OntologyTermSynonymMetaData.ID, atomId);
		entity.set(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM, atomName);
		return entity;
	}

	String concatenateListOfIds(List<String> ids)
	{
		return ids == null || ids.isEmpty() ? StringUtils.EMPTY : StringUtils.join(ids, MREF_SEPARATOR);
	}

	boolean isRoot(String hierachy)
	{
		return hierachy.split(NODEPATH_SEPARATOR).length == 1;
	}

	CsvWriter createCsvWriter(File outputFolder, EntityMetaData entityMetaData) throws IOException
	{
		CsvWriter csvWriter = new CsvWriter(
				new File(outputFolder.getAbsolutePath() + File.separator + entityMetaData.getName() + FILE_EXTENSION));

		csvWriter.writeAttributeNames(Lists.newArrayList(entityMetaData.getAtomicAttributes()).stream()
				.map(AttributeMetaData::getName).collect(Collectors.toList()));

		return csvWriter;
	}
}
