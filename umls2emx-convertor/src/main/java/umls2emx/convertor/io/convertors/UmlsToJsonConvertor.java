package umls2emx.convertor.io.convertors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonObject;

import umls2emx.convertor.beans.UmlsAtom;
import umls2emx.convertor.beans.UmlsConcept;
import umls2emx.convertor.io.iterators.UmlsConceptIterator;

public class UmlsToJsonConvertor implements UmlsConvertor
{
	private final static Log LOG = LogFactory.getLog(UmlsToJsonConvertor.class);
	private final static String FILE_NAME = "umlsAtomicNames.json";
	private static final String INDEX_NAME = "molgenis";
	private static final String DOCUMENT_TYPE = "umls";

	@Override
	public void convert(File outputFolder, File umlsConceptRelationFile, File umlsConceptHierachyFile,
			File umlsConceptFile, File umlsSemanticTypeFile, File umlsConceptSemanticTypeFile,
			boolean includeAnnotation)
	{
		try
		{
			File jsonOutputFile = new File(outputFolder.getAbsolutePath() + File.separator + FILE_NAME);
			BufferedWriter bufferedWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(jsonOutputFile), "UTF-8"));

			UmlsConceptIterator umlsConceptIterator = new UmlsConceptIterator(umlsConceptFile);
			while (umlsConceptIterator.hasNext())
			{
				bufferedWriter.write(createJsonString(umlsConceptIterator.next()));
			}
			umlsConceptIterator.close();
			bufferedWriter.close();
		}
		catch (IOException e)
		{
			LOG.error(e.getMessage());
		}
	}

	String createJsonString(UmlsConcept umlsConcept)
	{
		StringBuilder stringBuilder = new StringBuilder();
		String cuiId = umlsConcept.getCuiId();
		String preferredName = umlsConcept.getPreferredName();
		for (UmlsAtom umlsAtom : umlsConcept.getConceptAtoms())
		{
			JsonObject jsonId = new JsonObject();
			jsonId.addProperty("_id", UUID.randomUUID().toString());
			jsonId.addProperty("_index", INDEX_NAME);
			jsonId.addProperty("_type", DOCUMENT_TYPE);

			JsonObject jsonIndex = new JsonObject();
			jsonIndex.add("index", jsonId);

			JsonObject jsonData = new JsonObject();
			jsonData.addProperty("cui", cuiId);
			jsonData.addProperty("preferredName", preferredName);
			jsonData.addProperty("atomId", umlsAtom.getAtomId());
			jsonData.addProperty("ontologyTermSynonym", umlsAtom.getAtomName());
			jsonData.addProperty("source", umlsAtom.getAtomSource());

			stringBuilder.append(jsonIndex.toString()).append('\n').append(jsonData.toString()).append('\n');
		}

		return stringBuilder.toString();
	}
}
