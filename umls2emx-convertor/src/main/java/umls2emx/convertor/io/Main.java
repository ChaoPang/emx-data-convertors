package umls2emx.convertor.io;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import umls2emx.convertor.io.convertors.UmlsConvertor;
import umls2emx.convertor.io.convertors.UmlsToEmxConvertor;
import umls2emx.convertor.io.convertors.UmlsToJsonConvertor;

public class Main
{
	private final static String CONCEPT_FILE_NAME = "MRCONSO.RRF";
	private final static String CONCEPT_HIERACHY_FILE_NAME = "MRHIER.RRF";
	private final static String CONCEPT_RELATION_FILE_NAME = "MRREL.RRF";
	private final static String SEMANTIC_TYPE_FILE_NAME = "SemGroups.txt";
	private final static String CONCEPT_SEMANTIC_TYPE_FILE_NAME = "MRSTY.RRF";

	public static void main(String[] args) throws IOException, ParseException
	{
		Options options = createOptions();
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("input") && cmd.hasOption("output"))
		{
			boolean includeAnnotation = cmd.hasOption("include_annotation");

			File umlsInputFolder = new File(cmd.getOptionValue("input"));
			File umlsOutputFolder = new File(cmd.getOptionValue("output"));
			File umlsConceptFile = new File(umlsInputFolder.getAbsolutePath() + File.separator + CONCEPT_FILE_NAME);
			File umlsConceptHierachyFile = new File(
					umlsInputFolder.getAbsolutePath() + File.separator + CONCEPT_HIERACHY_FILE_NAME);
			File umlsConceptRelationFile = new File(
					umlsInputFolder.getAbsolutePath() + File.separator + CONCEPT_RELATION_FILE_NAME);
			File umlsSemanticTypeFile = new File(
					umlsInputFolder.getAbsolutePath() + File.separator + SEMANTIC_TYPE_FILE_NAME);
			File umlsConceptSemanticTypeFile = new File(
					umlsInputFolder.getAbsolutePath() + File.separator + CONCEPT_SEMANTIC_TYPE_FILE_NAME);
			UmlsConvertor umlsConvertor = cmd.hasOption("json_format") ? new UmlsToJsonConvertor()
					: new UmlsToEmxConvertor();
			umlsConvertor.convert(umlsOutputFolder, umlsConceptRelationFile, umlsConceptHierachyFile, umlsConceptFile,
					umlsSemanticTypeFile, umlsConceptSemanticTypeFile, includeAnnotation);
		}
		else
		{
			showHelpMessage(options);
		}
	}

	private static void showHelpMessage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar umlsConceptsToEmx.jar", "where options include:", options,
				"\nTo convert UMLS Rich Format to Emx format, it is suggested to increase the maximum amount of memory allocated to java e.g. -Xmx2G");
	}

	private static Options createOptions()
	{
		Options options = new Options();
		options.addOption(
				new Option("input", true, "provide the path of input folder where the umls files are stored"));
		options.addOption(new Option("output", true, "provide the path of output folder"));
		options.addOption(new Option("include_annotation", false,
				"a flag indicating whether or not to include the annotation in the output files"));
		options.addOption(new Option("json_format", false,
				"specify whether the data format of the output file is JSON. If this argument is not specified by default the data format will be EMX"));
		return options;
	}
}
