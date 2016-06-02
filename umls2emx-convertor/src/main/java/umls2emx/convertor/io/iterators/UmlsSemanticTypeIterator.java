package umls2emx.convertor.io.iterators;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

import umls2emx.convertor.beans.UmlsSemanticType;

public class UmlsSemanticTypeIterator implements Iterator<UmlsSemanticType>, Closeable
{
	private final static Logger LOG = Logger.getLogger(UmlsSemanticTypeIterator.class);
	private final static int SEMANTIC_TYPE_GROUP = 1;
	private final static int SEMANTIC_TYPE_IDENTIFIER = 2;
	private final static int SEMANTIC_TYPE_NAME = 3;
	private final static String FILE_SEPARATOR = "\\|";

	private LineIterator lineIterator;

	public UmlsSemanticTypeIterator(File umlsSemanticTypeFile)
	{
		try
		{
			lineIterator = FileUtils.lineIterator(umlsSemanticTypeFile, "UTF-8");
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
	public UmlsSemanticType next()
	{
		String currentLine = lineIterator.nextLine();
		String[] values = currentLine.split(FILE_SEPARATOR);

		return UmlsSemanticType.create(values[SEMANTIC_TYPE_IDENTIFIER], values[SEMANTIC_TYPE_NAME],
				values[SEMANTIC_TYPE_GROUP]);
	}

	@Override
	public void close() throws IOException
	{
		if (lineIterator != null) lineIterator.close();
	}
}