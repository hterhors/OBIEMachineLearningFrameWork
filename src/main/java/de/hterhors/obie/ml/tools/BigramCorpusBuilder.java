package de.hterhors.obie.ml.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.obie.core.ontology.AbstractOntologyEnvironment;
import de.hterhors.obie.core.ontology.OntologyInitializer;
import de.hterhors.obie.core.projects.AbstractProjectEnvironment;
import de.hterhors.obie.core.tools.corpus.CorpusFileTools;
import de.hterhors.obie.ml.corpus.BigramCorpusProvider;
import de.hterhors.obie.ml.ner.INamedEntitityLinker;

public class BigramCorpusBuilder {

	protected static Logger log = LogManager.getFormatterLogger(BigramCorpusBuilder.class);

	public static boolean overrideCorpusFileIfExists = false;

	public BigramCorpusBuilder(AbstractProjectEnvironment<?> projectEnvironment,
			final AbstractOntologyEnvironment ontologyEnvironment, INamedEntitityLinker linker) throws Exception {
		this(projectEnvironment, ontologyEnvironment, new HashSet<>(Arrays.asList(linker)));
	}

	public BigramCorpusBuilder(AbstractProjectEnvironment<?> projectEnvironment,
			final AbstractOntologyEnvironment ontologyEnvironment, Set<INamedEntitityLinker> linker) throws Exception {

		log.info("Override-flag was set to: " + overrideCorpusFileIfExists + ", "
				+ (overrideCorpusFileIfExists ? "existing corpus might be overriden!" : "corpus might not be saved."));

		final BigramCorpusProvider corpusProvider = new BigramCorpusProvider(projectEnvironment.getRawCorpusFile(),
				linker);

		storeCorpusToFile(corpusProvider, projectEnvironment, ontologyEnvironment.getOntologyVersion());

	}

	/**
	 * * Writes a corpus to the file-system.
	 * 
	 * @param corpus
	 * @param environment
	 * @param corpusPrefixName
	 */
	private void storeCorpusToFile(final BigramCorpusProvider corpus, AbstractProjectEnvironment<?> projectEnvironment,
			final int ontologyVersion) {

		final File corpusFile = CorpusFileTools.buildAnnotatedBigramCorpusFile(
				projectEnvironment.getBigramCorpusFileDirectory(), projectEnvironment.getCorpusName(),
				corpus.getOriginalRootClasses(), ontologyVersion);

		if (corpusFile.exists()) {
			log.warn("Corpus file already exists under name: " + corpusFile);

		}

		if (corpusFile.exists() && !overrideCorpusFileIfExists) {
			log.warn("Do not override, discard corpus!");
			return;
		} else {
			log.warn("Override file!");
		}

		corpusFile.getParentFile().mkdirs();

		log.info("Store corpus to " + corpusFile + "...");
		try {
			FileOutputStream fileOut;
			fileOut = new FileOutputStream(corpusFile);
			final ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(corpus);
			out.close();
			fileOut.close();
			log.info("Corpus successfully stored!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
