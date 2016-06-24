# micro-mid

This repository contains code for applying 
NLP methods to the Correlates of War (COW)
Military Interstate Dispute (MID) project 
(http://www.correlatesofwar.org/).  It relies 
heavily on the https://github.com/cmunell/micro-util
"micro-reading" NLP library, and the 
https://github.com/forkunited/micro-event event 
extraction, coref, and temporal ordering library.
It might be useful to read micro-util's README documentation
before you read this README to get an idea about why this
project is organized as it is.

## Setup and build ##

You can get the source code for the project by running

    git clone https://github.com/forkunited/micro-mid.git
    
and then build by running

    mvn compile 
    
from the root directory of the project assuming that you have
Maven setup and configured to access the internal RTW repository
as described at http://rtw.ml.cmu.edu/wiki/index.php?title=Maven.
If you don't have access to this wiki, then contact Bill for
instructions on how to configure Maven appropriately.

If you need to recompile with updated dependencies, then you should
run 

	mvn clean compile -U

If you want to pull the most recent version of the code, then
you should run

	git pull

## Configuration ##

You can use the *src/main/resources/mid.properties* file to configure
the project to reference data and resources at locations particular to
your system.  You should *not* modify this file in-place, but instead,
copy it to the top-level directory of the project, and modify it there
(so that your local system-specific modifications will not be checked into
the repository).  The code is set up to automatically read the file in 
the top-level directory if it finds it there. 

If you are on ds9, then you can modify *mid.properties* as follows:

	debug_dir=/data_reitter/micro/mid_debug
	storage_fs_bson_MIDBson=/data_reitter/micro/mid_bson
	storage_fs_str_MIDString=/data_reitter/micro/mid_string
	context_dir=[PATH TO THE MICRO-MID PROJECT]/src/main/resources/contexts/
	
	midNewsSvmUnlabeledDocumentCollectionName=mid_news_unlabeled
	midNewsSvmRelevanceLabeledDocumentCollectionName=mid_news_rel_labeled
	midNewsGoldRelevanceLabeledDocumentCollectionName=mid_news_gold_rel_labeled
	mid4CollectionName=mid4
	mid4NarrativeDocumentCollectionName=mid4_narratives
	mid4NarrativeEventMentionCollectionName=mid4_ev_mentions
	mid4NarrativeTimexCollectionName=mid4_timexes
	mid4NarrativeTimeValueCollectionName=mid4_tvalues

	word2vec_vectors_googlenews=/data_reitter/nlp_tools/word2vec/GoogleNews-vectors-negative300.bin.gz
	matetools_model_lemma=/data_reitter/nlp_tools/mateplus/models/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model
	matetools_model_parser=/data_reitter/nlp_tools/mateplus/models/CoNLL2009-ST-English-ALL.anna-3.3.parser.model
	matetools_model_tagger=/data_reitter/nlp_tools/mateplus/models/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model
	
	contextInputDirPath=
	experimentOutputDirPath=/data_reitter/micro/experiment/
	storageMongoMicroEventDatabaseName=micro_event
	storageFileSystemMicroEventDirPath=/data_reitter/micro/mid_bson/
	useMongoStorage=false
	
Note that the last 5 fields are deprecated, and unimportant.  The 4 prior to
the last 5 give paths to Word2Vec and MateTools models.  The 8 fields before those
(prefixed by "mid") give the names of data collections (sub-directories under the
location referenced by *storage_fs_bson_MIDBson*). The *context_dir* field
should be modified to point to the location of the ctx scripts in your project
copy of the project (under *src/main/resources/contexts*).  
*storage_fs_bson_MIDBson* points to the directory containing BSON data, and
 *storage_fs_bson_MIDString* points to a directory containing experiment 
 evaluation output and serialized trained models.

## Layout of the project ##

### Packages in the *src/main/java* directory ###

* *edu.psu.ist.acs.micro.mid.data* - tools for managing,
serializing, and serializing MID data.

* *edu.psu.ist.acs.micro.mid.data.annotation* - structures for
representing MID annotations and classes for building MID data sets.

* *edu.psu.ist.acs.micro.mid.data.annotation.nlp* - structures 
for representing NLP-specific annotations and data.  

* *edu.psu.ist.acs.micro.mid.data.model.nlp* - annotators for
providing MID related information about text documents based
on trained models (e.g. *MIDRelevanceAnnotator* annotates documents
with an indicator of whether or not they are MID relevant.  This
indicator type is defined in 
*edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID*,
and the annotator is run using
*edu.psu.ist.acs.micro.mid.scratch.RunMIDPipeline*.)

* *edu.psu.ist.acs.micro.mid.scratch* - various command-line
programs.  Some of these construct data sets, and
others perform the model training and evaluation experiments. 
Namely, classes in this package prefixed with *Construct* are used
to construct NLP annotated data sets. *RunMIDContext* runs ctx scripts
from *src/main/resources/contexts* to train models.  *RunMIDPipeline*
runs documents through NLP annotators and MID annotators in 
*edu.psu.ist.acs.micro.mid.model.annotator.nlp*.

* *edu.psu.ist.acs.micro.mid.util* - utilities and configuration

### Ctx experiments in the *src/main/resources/contexts* directory ###

The ctx scripts in *src/main/resources/contexts* are used to train and
evaluate new MID annotation models.  The scripts are partitioned into
the following sub-directories:

* *attribute* - for training MID attribute models (to predict
MID incident actions and their hostility levels described in text)

* *relevance* - for training MID relevance models (to predict
whether a given document is MID relevant)

* *util* - for utilities included by other scripts

* *old* - Old versions of scripts that are no longer used

The *attribute* and *relevance* directories are split into further 
sub-directories containing scripts for loading *data*, building feature
sets (*featureSets*), training classification *methods*, and performing
*evaluations* on those methods.  Each of these scripts is loaded through
a top-level *experiment* script in the *experiment* sub-directory.  An 
*experiment* script should be run using 
*edu.psu.ist.acs.micro.mid.scratch.RunMIDContext*.

Data for the experiments is generally loaded using scripts in the 
*data* sub-directory from "MIDBson" storage.  "MIDBson" storage is
in the directory specified by the *storage_fs_bson_MIDBson* property in 
the *mid.properties* configuration file (see the *Configuration* section 
above).  The evaluation results of the experiments are stored in 
"MIDString" storage using the scripts in the *experiment/output* 
sub-directory.  "MIDString" storage is in the directory specified by 
the *storage_fs_str_MIDString* property in the *mid.properties*
configuration file.

## Relevant data currently on ds9 ##

Correlates of War data 
(from http://www.correlatesofwar.org/data-sets/folder_listing)
 currently exists on ds9 in the following files and directories under
 */data_reitter/COW/*:
 
* *COW country codes.csv* - country codes from 
http://www.correlatesofwar.org/data-sets/cow-country-codes

* *Dyadic-MID*, *Incident-level*, *MID-level*, *MID3_Sources* -
MID4 data from http://www.correlatesofwar.org/data-sets/MIDs

* *Narratives* - MID narratives of MID4 data, also from 
http://www.correlatesofwar.org/data-sets/MIDs

* *MID_NLP_1993.tar.gz*, *NewsGold20000* - Part of the data used
to train Vito's old MID relevance SVM

* *News* - MID5 news data classified by Vito's old SVM.  It's split
into hand annotated SVM true and false positives, and non-hand 
annotated (unlabeled) SVM negatives.

* *nlp2011* - News data published in 2011

The Correlates of War data has been annotated with some NLP annotations,
and stored in BSON format in */data_reitter/micro/mid_bson/* in the 
following directories:

* *mid4*, *mid4_timexes*, *mid4_tvalues*, *mid4_ev_mentions*, 
*mid4_narratives* - MID4 disputes, time expressions, event mentions,
and annotated narrative documents created by running 
*edu.psu.ist.acs.micro.mid.scratch.ConstructCOWData* on data
in */data_reitter/COW/Narratives*.

* *mid4_narratives_test*, *mid4_narratives_dev*, *mid4_narratives_train* 
- Partition of *mid4_narratives* data into train/dev/test split created
by running *edu.psu.ist.acs.micro.mid.scratch.SplitCOWData*

* *mid_news_gold_rel_labeled_tokens* - Tokenized documents created by
running *edu.psu.ist.acs.micro.mid.scratch.ConstructMID4NewsDocumentSet* 
on *NewsGold20000* (I recommend *not* using this in the future.  It's
messy.  See documentation in *ConstructMID4NewsDocumentSet* for more
details.

* *mid_news_rel_labeled_POS* and *mid_news_unlabeled_POS* - These are
tokenized and pos tagged documents created by running 
*edu.psu.ist.acs.micro.mid.scratch.ConstructMID5SVMNewsDocumentSet* on
*/data_reitter/COW/News*.  Similar directories suffixed with "tokens"
in place of "POS" were generated the same way, but are only tokenized
without part-of-speech annotations.

- The remaining directories with names prefixed by *mid_news_rel* contain
splits various splits of the data in *mid_news_rel_labeled_POS* and 
*mid_news_unlabeled_POS*.  These splits were created by using
*edu.psu.ist.acs.micro.mid.scratch.RunMIDContext* to run the *Full.ctx* and
*SkewedUnlabeled.ctx* ctx scripts in 
*src/main/resources/contexts/relevance/data/construct/*.  They were 
used to train
different versions of the MID relevance classification model using the 
ctx scripts in *src/main/resources/contexts/relevance/experiment*.

## MID classification models currently on ds9 ##

Serialized models for classifying documents by their MID 
relevance and attributes currently exist on ds9 in the
*/data_reitter/micro/projects/micro-mid-data/src/main/resources/models/* 
directory. They are:

* *Relevance_Test_StanfordLinearCTOpt_BiasedNoProperNoun2_Unlabeled100* -
This model determines whether or not a document is MID relevant.  It was
created using the ctx script 
*Test_StanfordLinearCTOpt_BiasedNoProperNoun2_Unlabeled100.ctx* in 
*src/main/resources/contexts/relevance/experiment/*.  This script trains
a Stanford CoreNLP binary maximum entropy (i.e. logistic regression) model
with bigram bag-of-words features excluding proper nouns.  The training data
consisted of 1 percent (human true) positives and 99 percent (human 
unlabeled) negatives from Vito's old SVM.  This data is in the 
*mid_news_rel_[train/dev/test]_data_pu100* directories under 
*/data_reitter/micro/mid_bson/*.  Classifications are produced by this model
using *edu.psu.ist.acs.micro.mid.model.annotator.nlp.MIDRelevanceAnnotator*.

* *Attribute_Test_StanfordLinearCTOpt* - This model determines whether or not
a document has various MID action and hostility level attributes defined by
the enum types in *edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident*.  
It was created using the ctx script *Test_StanfordLinearCTOpt.ctx* in 
*src/main/resources/contexts/attribute/experiment/*.  The script trains a
Stanford CoreNLP binary maximum entropy (i.e. logistic regression) model
for each attribute.  The training data consisted of labeled example narratives
from MID4 data.  This data is in the *mid4_narratives_[train/dev/test]* 
directories under */data_reitter/micro/mid_bson*.  Classifications are 
produced by this model using 
*edu.psu.ist.acs.micro.mid.model.annotator.nlp.MIDAttributeAnnotator*. 

These serialized models are both included in the *micro-mid-data* Maven 
project at */data_reitter/micro/projects/micro-mid-data/*.  When they are updated,
they should be redeployed using the command *mvn deploy* run from
the project's top-level directory.  The *micro-mid* project includes these
serialized models through its dependency on *micro-mid-data*.  They are 
loaded into memory by the *MIDAttributeAnnotator* and *MIDRelevanceAnnotator* 
classes when annotating documents as part of the pipeline run using 
*edu.psu.ist.acs.micro.mid.scratch.RunMIDPipeline*.

## Execution ##

You can run the classes in *edu.psu.ist.acs.micro.mid.scratch* using the Maven
exec:java directive.  For example, the MID annotation pipeline can be run 
using a script of the following form:

	#!/bin/bash

	cd [path/to/micro-mid]

	INPUT=[path/to/input/directory/or/file]
	MAX_THREADS=[maximum number of threads]
	STORAGE_DIR=[path/to/output/storage/directory]
	HTML_STORAGE_DIR=[path/to/output/html/storage/directory]
	OUTPUT_RELEVANCE_FILE=[path/to/output/mid/relevance/classification/file]
	PROPERTIES_FILE=[path/to/mid.properties/configuration/file]

	export MAVEN_OPTS=-Xmx[gigs of heap to reserve]G
	git pull
	mvn clean compile -U

	mvn exec:java -Dexec.mainClass="edu.psu.ist.acs.micro.mid.scratch.RunMIDPipeline" -Dexec.args="--input=${INPUT} --maxThreads=${MAX_THREADS} --storageDir=${STORAGE_DIR} --htmlStorageDir=${HTML_STORAGE_DIR} --outputRelevanceFile=${OUTPUT_RELEVANCE_FILE} --propertiesFile=${PROPERTIES_FILE}"

Similarly, you can run a ctx script experiment to train and evaluate a MID relevance
model using a script of this form:

	#!/bin/bash

	cd [path/to/micro-mid]

	CONTEXT_DIR=[path/to/ctx/script/directory/i.e./src/main/resources/contexts]

	export MAVEN_OPTS=-Xmx[gigs of heap to reserve]G
	git pull
	mvn clean compile -U

	mvn exec:java -Dexec.mainClass="edu.psu.ist.acs.micro.mid.scratch.RunMIDContext" -Dexec.args="${CONTEXT_DIR}/relevance/experiment/[experiment-to-run].ctx"

### Building a jar ###

If you need to run things on a server that doesn't have Maven, then it might be necessary
to build a jar to run.  You can build a fat jar containing all dependencies by running

	mvn clean compile assembly:single
	
The jar will be output to the *target* directory.  

### Training new models ###

If you want to train new classifiers on the MID data, you can setup new ctx script
experiments in the *src/main/resources/contexts* directory, and run them using 
*edu.psu.ist.acs.micro.mid.scratch.RunMIDContext*.
