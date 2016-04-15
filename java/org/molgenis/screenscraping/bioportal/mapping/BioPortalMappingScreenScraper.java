package org.molgenis.screenscraping.bioportal.mapping;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.support.MapEntity;

public class BioPortalMappingScreenScraper
{
	public static void main(String[] args) throws IOException, ParseException
	{
		Options options = createOptions();
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("target") && cmd.hasOption("source") && cmd.hasOption("page") && cmd.hasOption("output"))
		{
			String targetOntology = cmd.getOptionValue("target");
			String sourceOntology = cmd.getOptionValue("source");
			String page = cmd.getOptionValue("page");
			String outputPath = cmd.getOptionValue("output");

			CsvWriter writer = new CsvWriter(new File(outputPath));
			writer.writeAttributeNames(Arrays.asList(sourceOntology, targetOntology));

			for (int index = 1; index <= Integer.parseInt(page); index++)
			{
				URL url = new URL("http://bioportal.bioontology.org/mappings/show/" + targetOntology
						+ "?target=http://data.bioontology.org/ontologies/" + sourceOntology
						+ "&apikey=4d7ac5ac-789a-4d7f-b4e1-8478e87e2f3c&page=" + index);
				Document doc = null;
				while (doc == null)
				{
					try
					{
						doc = Jsoup.parse(url, 100000);
					}
					catch (IOException e)
					{
						doc = null;
					}
				}

				for (Element table : doc.select("table"))
				{
					MapEntity entity = new MapEntity();
					for (Element tr : table.select("tr:not(thead tr)"))
					{
						Elements columns = tr.select("td");

						for (int i = 0; i < columns.size(); i++)
						{
							Elements hrefs = columns.get(i).select("a");
							if (hrefs.size() > 0)
							{
								String ontology;
								if (i == 0)
								{
									ontology = sourceOntology;
								}
								else
								{
									ontology = targetOntology;
								}

								String hrefValue = hrefs.get(0).attr("href");
								String[] split = hrefValue.split("conceptid=");

								String bioPortalClassLabel = getBioPortalClassLabel(ontology, split[1]);
								System.out.println(ontology + ":" + bioPortalClassLabel);

								entity.set(ontology, bioPortalClassLabel);
							}
						}

						writer.add(entity);
					}
				}
			}

			writer.close();
		}
		else
		{
			showHelpMessage(options);
		}
	}

	private static void showHelpMessage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"java -jar distance.jar",
						"where options include:",
						options,
						"\nTo calculate the distance matrix, it is suggested to increase the maximum amount of memory allocated to java e.g. -Xmx2G");
	}

	private static Options createOptions()
	{
		Options options = new Options();
		options.addOption(new Option("target", true, "The acronym of target ontology"));
		options.addOption(new Option("source", true, "The acronym of source ontology that is matched to target"));
		options.addOption(new Option("page", true, "The total number of pages in mapping table"));
		options.addOption(new Option("output", true, "provide the path of output file"));
		return options;
	}

	public static String getBioPortalClassLabel(String ontology, String conceptId) throws ClientProtocolException,
			IOException
	{
		String url = "http://data.bioontology.org/ontologies/" + ontology + "/classes/" + conceptId
				+ "?apikey=4d7ac5ac-789a-4d7f-b4e1-8478e87e2f3c&format=json";
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet httpGet = new HttpGet(url);
		HttpResponse httpResponse = httpClient.execute(httpGet);
		String responseString = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
		JSONObject object = new JSONObject(responseString);
		String label;
		if (object.has("prefLabel"))
		{
			label = object.getString("prefLabel");
		}
		else
		{
			label = conceptId;
		}
		return label;
	}
}
