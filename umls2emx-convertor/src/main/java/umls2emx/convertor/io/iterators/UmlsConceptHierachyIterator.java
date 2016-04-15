package umls2emx.convertor.io.iterators;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.data.support.UuidGenerator;

import autovalue.shaded.com.google.common.common.collect.Lists;
import umls2emx.convertor.beans.UmlsAtom;
import umls2emx.convertor.beans.UmlsHierachicalRelation;

public class UmlsConceptHierachyIterator implements Iterator<UmlsHierachicalRelation>, Closeable
{
	private final static Logger LOG = Logger.getLogger(UmlsConceptHierachyIterator.class);
	private final static int CONCEPT_ID_INDEX = 0;
	private final static int ATOM_SOURCE = 4;
	private final static int ATOM_HIERACHY = 6;
	private final static String FILE_SEPARATOR = "\\|";
	private final static UuidGenerator UUID_GENERATOR = new UuidGenerator();

	private String prevCuiId = null;
	private String prevLine = null;
	private LineIterator lineIterator;
	private List<String> prevRelatedLines;

	public UmlsConceptHierachyIterator(File file)
	{
		try
		{
			lineIterator = FileUtils.lineIterator(file, "UTF-8");
			prevRelatedLines = new ArrayList<>();
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
	public UmlsHierachicalRelation next()
	{
		try
		{
			List<UmlsAtom> umlsAtoms = prevRelatedLines.stream().map(this::createUmlsAtom)
					.filter(umlsAtom -> StringUtils.isNotBlank(umlsAtom.getHierachy())).collect(Collectors.toList());
			UmlsHierachicalRelation umlsHierachicalRelation = UmlsHierachicalRelation
					.create(getCuiId(prevRelatedLines.get(0)), umlsAtoms);
			prevRelatedLines.clear();
			return umlsHierachicalRelation;
		}
		catch (Exception exception)
		{
			throw new RuntimeException("The problem occurs at the lines: \n" + prevRelatedLines.toString());
		}
	}

	String getCuiId(String line)
	{
		String[] split = line.split(FILE_SEPARATOR);
		return split.length > 0 ? split[CONCEPT_ID_INDEX] : StringUtils.EMPTY;
	}

	UmlsAtom createUmlsAtom(String line)
	{
		List<String> collect = Lists.newArrayList(line.split(FILE_SEPARATOR)).stream().collect(Collectors.toList());
		String atomId = UUID_GENERATOR.generateId();
		String atomSource = collect.get(ATOM_SOURCE);
		String atomHierachy = collect.size() > ATOM_HIERACHY ? collect.get(ATOM_HIERACHY) : StringUtils.EMPTY;
		return UmlsAtom.create(atomId, StringUtils.EMPTY, atomSource, atomHierachy);
	}

	@Override
	public void close() throws IOException
	{
		if (lineIterator != null) lineIterator.close();
	}
}
