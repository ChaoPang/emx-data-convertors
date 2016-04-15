package umls2emx.convertor.io.iterators;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

import autovalue.shaded.com.google.common.common.collect.Lists;
import umls2emx.convertor.beans.UmlsRelation;
import umls2emx.convertor.beans.UmlsRelationAttribute;

public class UmlsConceptRelationIterator implements Iterator<UmlsRelation>, Closeable
{
	private final static Logger LOG = Logger.getLogger(UmlsConceptRelationIterator.class);

	private final static int CONCEPT_ONE_ID_INDEX = 0;
	private final static int ATOM_ONE_ID_INDEX = 1;
	private final static int CONCEPT_TWO_ID_INDEX = 4;
	private final static int ATOM_TWO_ID_INDEX = 5;
	private final static int RELATION_LABEL_INDEX = 7;
	private final static int RELATION_IDENTIFIER_IDENTIFIER = 8;

	private final static String FILE_SEPARATOR = "\\|";

	private LineIterator lineIterator;

	public UmlsConceptRelationIterator(File file)
	{
		try
		{
			lineIterator = FileUtils.lineIterator(file, "UTF-8");
		}
		catch (IOException e)
		{
			LOG.error(e.getMessage());
			LineIterator.closeQuietly(lineIterator);
		}
	}

	@Override
	public boolean hasNext()
	{
		return lineIterator.hasNext();
	}

	@Override
	public UmlsRelation next()
	{
		return createUmlsRelation(lineIterator.next());
	}

	UmlsRelation createUmlsRelation(String line)
	{
		List<String> collect = Lists.newArrayList(line.split(FILE_SEPARATOR)).stream().collect(Collectors.toList());
		String cuiId1 = collect.get(CONCEPT_ONE_ID_INDEX);
		String atomId1 = collect.get(ATOM_ONE_ID_INDEX);
		String cuiId2 = collect.get(CONCEPT_TWO_ID_INDEX);
		String atomId2 = collect.get(ATOM_TWO_ID_INDEX);
		String relationIdentifier = collect.get(RELATION_IDENTIFIER_IDENTIFIER);
		String relationLabel = collect.get(RELATION_LABEL_INDEX);

		return UmlsRelation.create(relationIdentifier, cuiId1, atomId1, UmlsRelationAttribute.create(relationLabel),
				cuiId2, atomId2);
	}

	@Override
	public void close() throws IOException
	{
		if (lineIterator != null) lineIterator.close();
	}
}
