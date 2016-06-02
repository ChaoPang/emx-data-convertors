package umls2emx.convertor.io.iterators;

import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.data.support.UuidGenerator;

import autovalue.shaded.com.google.common.common.collect.Lists;
import umls2emx.convertor.beans.Synonym;
import umls2emx.convertor.beans.UmlsAtom;
import umls2emx.convertor.beans.UmlsConcept;

public class UmlsConceptIterator implements Iterator<UmlsConcept>, Closeable
{
	private final static Logger LOG = Logger.getLogger(UmlsConceptIterator.class);
	private final static int CONCEPT_ID_INDEX = 0;
	private final static int ATOM_ID_INDEX = 7;
	private final static int ATOM_NAME_INDEX = 14;
	private final static int ATOM_SOURCE_INDEX = 11;
	private final static String FILE_SEPARATOR = "\\|";

	private UuidGenerator idGenenrator = new UuidGenerator();
	private String prevCuiId = null;
	private String prevLine = null;
	private LineIterator lineIterator;
	private List<String> prevRelatedLines;

	public UmlsConceptIterator(File umlsConceptFile)
	{
		try
		{
			lineIterator = FileUtils.lineIterator(umlsConceptFile, "UTF-8");
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
				// collect
				// such lines with the same cuiId
				if (currentCuiId.equals(prevCuiId))
				{
					prevRelatedLines.add(currentLine);
				}
				else
				{
					// If the current line does not have the same cuiId as the previous ones, break this round of
					// iteration
					prevLine = currentLine;
					prevCuiId = currentCuiId;
					break;
				}
			}
		}

		return !prevRelatedLines.isEmpty();
	}

	@Override
	public UmlsConcept next()
	{
		List<UmlsAtom> umlsAtoms = prevRelatedLines.stream().map(this::createUmlsAtom).collect(toList());
		List<Synonym> synonyms = umlsAtoms.stream()
				.map(atom -> Synonym.create(idGenenrator.generateId(), atom.getAtomName())).collect(toList());
		UmlsConcept umlsConcept = UmlsConcept.create(getCuiId(prevRelatedLines.get(0)), umlsAtoms.get(0).getAtomName(),
				umlsAtoms, reduceSynonyms(synonyms));
		prevRelatedLines.clear();
		return umlsConcept;
	}

	String getCuiId(String line)
	{
		String[] split = line.split(FILE_SEPARATOR);
		return split.length > 0 ? split[CONCEPT_ID_INDEX] : StringUtils.EMPTY;
	}

	UmlsAtom createUmlsAtom(String line)
	{
		List<String> collect = Lists.newArrayList(line.split(FILE_SEPARATOR));
		String atomId = collect.get(ATOM_ID_INDEX);
		String atomName = collect.get(ATOM_NAME_INDEX);
		String atomSource = collect.get(ATOM_SOURCE_INDEX);
		return UmlsAtom.create(atomId, atomName, atomSource);
	}

	List<Synonym> reduceSynonyms(List<Synonym> synonyms)
	{
		List<Synonym> reducedSynonyms = new ArrayList<>();
		Set<String> lowercasedSynonyms = new HashSet<>();
		for (Synonym synonym : synonyms)
		{
			String lowercase = synonym.getName().toLowerCase();
			if (!lowercasedSynonyms.contains(lowercase))
			{
				reducedSynonyms.add(synonym);
				lowercasedSynonyms.add(lowercase);
			}
		}
		return reducedSynonyms;
	}

	@Override
	public void close() throws IOException
	{
		LineIterator.closeQuietly(lineIterator);
	}
}