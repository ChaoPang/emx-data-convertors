package org.molgenis.sorta_convert;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.molgenis.data.MolgenisInvalidFormatException;

/**
 * Hello world!
 *
 */
public class App
{
	public static void main(String[] args) throws ParseException, IOException, MolgenisInvalidFormatException
	{
		Options options = createOptions();
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		if (cmd.hasOption("input") && cmd.hasOption("output"))
		{
			String inputFilePath = cmd.getOptionValue("input");
			String outputFolder = cmd.getOptionValue("output");
			File inputFile = new File(inputFilePath);
			File outputFolderFile = new File(outputFolder);

			if (!inputFile.exists())
			{
				System.out.println("The provided input file " + inputFilePath + " does not exist!");
				System.exit(0);
			}
			if (!outputFolderFile.exists())
			{
				System.out.println("The provided output folder " + outputFolder + " does not exist!");
				System.exit(0);
			}

			File outputFile = new File(
					outputFolderFile.getAbsolutePath() + File.separator + inputFile.getName() + ".zip");

			SortaEmxConvertor converter = new SortaEmxConvertor();
			if (cmd.hasOption("excel_format"))
			{
				converter.convertExcelFile(inputFile, outputFile);
			}
			else
			{
				converter.convertCsvFile(inputFile, outputFile);
			}
		}
		else
		{
			showHelpMessage(options);
		}
	}

	private static void showHelpMessage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar sortEmxConvertor.jar", "where options include:", options,
				"\nTo convert the SORTA terminology data format to the Emx format");
	}

	private static Options createOptions()
	{
		Options options = new Options();
		options.addOption(new Option("input", true, "the input file that is to be converted to EMX format"));
		options.addOption(new Option("output", true, "the output folder where you want to store the converted data"));
		options.addOption(new Option("excel_format", false,
				"a flag indicating whether or not to the file is the Excel format. If this option is not included, the file is considered as the CSV format by default"));
		return options;
	}
}
