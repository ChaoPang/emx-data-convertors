package org.finnish.data.convertor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.util.ZipUtils;
import org.molgenis.util.ZipUtils.DirectoryStructure;

public class Main
{
	public static void main(String[] args) throws IOException, ParseException
	{
		Options options = createOptions();
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("packageName") && cmd.hasOption("entityName") && cmd.hasOption("inputFile")
				&& cmd.hasOption("outputFolder"))
		{
			String packageName = cmd.getOptionValue("packageName");
			String entityName = cmd.getOptionValue("entityName");
			File inputFile = new File(cmd.getOptionValue("inputFile"));
			File outputFolder = new File(cmd.getOptionValue("outputFolder"));

			if (StringUtils.isBlank(packageName))
			{
				System.out.println("The package name cannot be empty!");
				System.exit(0);
			}

			if (StringUtils.isBlank(entityName))
			{
				System.out.println("The entityName name cannot be empty!");
				System.exit(0);
			}

			if (!inputFile.exists())
			{
				System.out.println("The input file does not exist!");
				System.exit(0);
			}
			if (!outputFolder.exists())
			{
				System.out.println("The output folder does not exist!");
				System.exit(0);
			}

			List<String> filePaths = new ArrayList<>();
			FinnishMetaDataToEmxMetaData finnishMetaDataToEmxMetaData = new FinnishMetaDataToEmxMetaData(filePaths);
			finnishMetaDataToEmxMetaData.convert(packageName, entityName, inputFile, outputFolder);

			List<File> filesToZip = filePaths.stream().map(FileUtils::getFile).collect(Collectors.toList());
			File zipFile = new File(outputFolder.getAbsolutePath() + File.separator + entityName + ".zip");
			ZipUtils.compress(filesToZip, zipFile, DirectoryStructure.EXCLUDE_DIR);
			filesToZip.stream().forEach(FileUtils::deleteQuietly);
		}
		else
		{
			showHelpMessage(options);
		}
	}

	private static void showHelpMessage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar finnishDataFormatToEmx.jar", "where options include:", options,
				"\nTo convert the finnish data format to Emx format.");
	}

	private static Options createOptions()
	{
		Options options = new Options();
		options.addOption(new Option("packageName", true,
				"provide the package name where all of the metadata will be stored. The package serves as container or alternatively is referred to as the project."));
		options.addOption(new Option("entityName", true,
				"provide the entity name for the metadata you want to upload to molgenis/biobankconnect, this will be used as the name of your metadata in the system."));
		options.addOption(new Option("inputFile", true,
				"The input file that contains all the metadata in the Finnish data format."));
		options.addOption(
				new Option("outputFolder", true, "the destination folder where the converted data will be saved to."));
		return options;
	}
}
