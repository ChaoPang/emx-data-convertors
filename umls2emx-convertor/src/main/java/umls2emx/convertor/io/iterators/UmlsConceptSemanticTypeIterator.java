package umls2emx.convertor.io.iterators;

import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.data.support.UuidGenerator;

import umls2emx.convertor.beans.UmlsConceptSemanticType;

public class UmlsConceptSemanticTypeIterator implements Iterator<UmlsConceptSemanticType>, Closeable
{
	private final static Logger LOG = Logger.getLogger(UmlsSemanticTypeIterator.class);
	private final static int COMCEPT_ID = 0;
	private final static int SEMANTIC_TYPE_IDENTIFIER = 1;
	private final static String FILE_SEPARATOR = "\\|";

	private String prevCuiId = null;
	private String prevLine = null;
	private LineIterator lineIterator;
	private List<String> prevRelatedLines;
	private Set<String> validSemanticTypeIdentifiers;
	private UuidGenerator idGenenrator = new UuidGenerator();

	public UmlsConceptSemanticTypeIterator(File conceptUmlsSemanticTypeFile, Set<String> validSemanticTypeIdentifiers)
	{
		try
		{
			this.lineIterator = FileUtils.lineIterator(conceptUmlsSemanticTypeFile, "UTF-8");
			this.validSemanticTypeIdentifiers = validSemanticTypeIdentifiers;
			this.prevRelatedLines = new ArrayList<>();
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
		if (StringUtils.isNotBlank(prevLine))
		{
			prevRelatedLines.add(prevLine);
			prevLine = null;
		}

		// Iterate through the file iterator
		while (lineIterator.hasNext())
		{
			String currentLine = lineIterator.nextLine();

			if (StringUtils.isNotBlank(currentLine))
			{
				String currentCuiId = getCuiId(currentLine);
				// First time, assign a value to prevCuiId
				if (StringUtils.isBlank(prevCuiId))
				{
					prevCuiId = currentCuiId;
				}

				// We check if the previous cuiId is equal to the current cuiId, if they are the same, we need to
				// collect such lines with the same cuiId
				if (currentCuiId.equals(prevCuiId))
				{
					prevRelatedLines.add(currentLine);
				}
				else
				{
					// If the current line does not have the same cuiId as the previous ones, we need to release the
					// previous related lines.
					prevLine = currentLine;
					prevCuiId = currentCuiId;
					break;
				}
			}
		}

		return !prevRelatedLines.isEmpty();
	}

	@Override
	public UmlsConceptSemanticType next()
	{
		try
		{
			List<String> semanticTypeIds = prevRelatedLines.stream()
					.map(line -> line.split(FILE_SEPARATOR)[SEMANTIC_TYPE_IDENTIFIER])
					.filter(validSemanticTypeIdentifiers::contains).collect(toList());

			UmlsConceptSemanticType umlsHierachicalRelation = UmlsConceptSemanticType.create(idGenenrator.generateId(),
					getCuiId(prevRelatedLines.get(0)), semanticTypeIds);

			prevRelatedLines.clear();
			return umlsHierachicalRelation;
		}
		catch (Exception exception)
		{
			throw new RuntimeException("The problem occurs at the lines: \n" + prevRelatedLines.toString());
		}
	}

	@Override
	public void close() throws IOException
	{
		if (lineIterator != null) lineIterator.close();
	}

	String getCuiId(String line)
	{
		String[] split = line.split(FILE_SEPARATOR);
		return split.length > 0 ? split[COMCEPT_ID] : StringUtils.EMPTY;
	}

}
