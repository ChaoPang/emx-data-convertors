package umls2emx.convertor.io.convertors;

import java.io.File;

public interface UmlsConvertor
{
	abstract void convert(File inputFile, File umlsConceptRelationFile, File umlsConceptHierachyFile,
			File umlsConceptFile, File umlsSemanticTypeFile, File umlsConceptSemanticTypeFile,
			boolean includeAnnotation);
}
