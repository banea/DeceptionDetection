import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Created by carmennb on 4/21/16.
 */
public class DeceptionDatasetBuilder {
    static Logger logger;

    public static void main(String[] args) throws IOException,
            ClassNotFoundException{
        DeceptionDatasetBuilder myBuilder = new DeceptionDatasetBuilder();

        /** Initialize logger instance */
        logger = LoggerFactory.getLogger(new Object() {
        }.getClass());
        try {
            /** The log config file path. */
            //String packagePath = "edu/cornell/TextualSimilarity/";
            String logConfigFilePath = "src/main/resources/log4j.file.properties";
            DataInputStream rf = new DataInputStream(new FileInputStream(logConfigFilePath));
            PropertyConfigurator.configure(rf);
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }


        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        props.setProperty("ssplit.isOneSentence", "true");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //load the dataset
        String datasetPath = "src/main/resources/dataset.csv";
        logger.info("Loading dataset from " + datasetPath + "...");
        Reader in = new FileReader(datasetPath);
        CSVParser parser = new CSVParser(in, CSVFormat.DEFAULT.withHeader());
        List<CSVRecord> records = parser.getRecords();
        logger.debug("Size "+ records.size());

        List<DeceptionAnnotation> dataset = new ArrayList<DeceptionAnnotation>();
        int count = 0;
        for (CSVRecord record: records) {
            //if (count< 14){count++;}
            //else {break;}
            //id=0, _gender=1, age=2, education=3, country=4, text=5, class=6
            if (record.size() == 7){ //if correct CSV parsing
                DeceptionAnnotation instance = new DeceptionAnnotation(
                    record.get(0), record.get(1), Integer.parseInt(record.get(2)), record.get(3),
                    record.get(4), record.get(5), record.get(6));
                dataset.add(instance);
            } else { //if incorrect parsing, repair
                StringBuilder myText = new StringBuilder();
                for (int i = 5; i < record.size()-1; i++) {
                    myText.append(record.get(i));
                    if (record.size() - 2 > i ){
                        myText.append(",");
                    }
                }
                DeceptionAnnotation instance = new DeceptionAnnotation(
                        record.get(0), record.get(1), Integer.parseInt(record.get(2)), record.get(3),
                        record.get(4), myText.toString(), record.get(record.size()-1));
                dataset.add(instance);
            }
        }

        logger.info("Done. Processing dataset...");
        List<DeceptionInstance> instances = myBuilder.processDataset(dataset, pipeline);
        logger.info("Done. Found " + instances.size() + " deception instances");
        List<DeceptionInstance> instancesPerUser = myBuilder.generateTwoInstancesPerUser(instances);
        logger.info("Done. Found " + instancesPerUser.size() + " user instances");
        File arff = new File ("output/overall.arff");
        logger.info("Preparring to write to arff formatted file " + arff.toString() );
        myBuilder.printDataset(instancesPerUser, arff);
        logger.info("Arff dataset generated.");
    }


    private List<DeceptionInstance> generateTwoInstancesPerUser(List<DeceptionInstance> instances){
        List<DeceptionInstance> instancesPerUser = new ArrayList<DeceptionInstance>();

        int i = 0;
        StringBuilder sentences = new StringBuilder();
        StringBuilder pos = new StringBuilder();
        StringBuilder unlexProdRules = new StringBuilder();
        StringBuilder lexProdRules = new StringBuilder();
        StringBuilder unlexProdRulesWGrandparent = new StringBuilder();
        StringBuilder lexProdRulesWGrandparent = new StringBuilder();

        while (i < instances.size()){

            if (i % 7 == 0){ //new instance
                //initialize
                sentences = new StringBuilder();
                pos = new StringBuilder();
                unlexProdRules =  new StringBuilder();
                lexProdRules = new StringBuilder();
                unlexProdRulesWGrandparent = new StringBuilder();
                lexProdRulesWGrandparent = new StringBuilder();
            }

            sentences.append(instances.get(i).getSentence()).append(" ");
            pos.append(instances.get(i).getPOS()).append(" ");
            unlexProdRules.append(instances.get(i).getUnlexProdRules()).append(" ");
            lexProdRules.append(instances.get(i).getLexProdRules()).append(" ");
            unlexProdRulesWGrandparent.append(instances.get(i).getUnlexProdRulesWGrandparent()).append(" ");
            lexProdRulesWGrandparent.append(instances.get(i).getLexProdRulesWGrandparent()).append(" ");

            if (i % 7 == 6){ //end instance
                String key = instances.get(i).getUniqueIdentifier();
                DeceptionInstance mappedInstance = new DeceptionInstance(key.substring(0,key.length()-2), instances.get(i).getMyClass());
                mappedInstance.setSentence(sentences.toString().trim());
                mappedInstance.setPOS(pos.toString().trim());
                mappedInstance.setUnlexProdRules(unlexProdRules.toString().trim());
                mappedInstance.setLexProdRules(lexProdRules.toString().trim());
                mappedInstance.setUnlexProdRulesWGrandparent(unlexProdRulesWGrandparent.toString().trim());
                mappedInstance.setLexProdRulesWGrandparent(lexProdRulesWGrandparent.toString().trim());
                instancesPerUser.add(mappedInstance);
                logger.debug("Added instance "+mappedInstance.toString());
            }
            i = i+1;
        }

        return instancesPerUser;
    }


    private void printDataset(List<DeceptionInstance> instances, File arff) throws IOException {
        arff.createNewFile();
        FileWriter fw = new FileWriter(arff.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(DeceptionInstance.getArffHeader());
        for (DeceptionInstance currInstance : instances){
            bw.write(currInstance.getArffString()+"\n");
        }
        bw.close();
    }


    public List<DeceptionInstance> processDataset (List<DeceptionAnnotation> dataset, StanfordCoreNLP pipeline ){
        List<DeceptionInstance> instances = new ArrayList<DeceptionInstance>();

        for (DeceptionAnnotation myRecord : dataset){
            StringBuilder words = new StringBuilder();
            StringBuilder pos = new StringBuilder();
            StringBuilder unlexProdRules = new StringBuilder();
            StringBuilder lexProdRules = new StringBuilder();
            StringBuilder unlexProdRulesWGrandparent = new StringBuilder();
            StringBuilder lexProdRulesWGrandparent = new StringBuilder();
            String text = myRecord.getText();

            //sentenceWrapper -> contains the actual sentence
            //remove the quotation marks
            Annotation sentenceWrapper = new Annotation(text.substring(1,text.length()-1));
            pipeline.annotate(sentenceWrapper);
            //and the list of sentences has a single element (namely the sentence)
            List<CoreMap> sentences = sentenceWrapper.get(CoreAnnotations.SentencesAnnotation.class);
            if (sentences.size() != 1){
                logger.error("More than one sentence found!");
            }
            List<CoreLabel> tokens = sentences.get(0).get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens){
                //append the word
                words.append(token.get(CoreAnnotations.TextAnnotation.class)).append(" ");
                //append the pos
                pos.append(token.get(CoreAnnotations.PartOfSpeechAnnotation.class)).append(" ");
            }

            // this is the constituency parse tree of the current sentence
            Tree tree = sentences.get(0).get(TreeCoreAnnotations.TreeAnnotation.class);

            Iterator<Tree> myIterator = tree.iterator();
            while (myIterator.hasNext()) {
                Tree subTree = myIterator.next();
                if (subTree.isPreTerminal()){ //a preterminal has a single leaf DT->the
                    Tree[] leafs = subTree.children();
                    //update lexProdRules
                    lexProdRules.append("*").append(subTree.label()).append("->");
                    lexProdRules.append(leafs[0].toString().toLowerCase()).append(" ");
                    //update lexProdRulesWGrandparent
                    lexProdRulesWGrandparent.append("*").append(subTree.label()).append("^");
                    lexProdRulesWGrandparent.append(subTree.parent(tree).label()).append("->");
                    lexProdRulesWGrandparent.append(leafs[0].toString().toLowerCase()).append(" ");
                    //logger.debug("*" + subTree.label() + "^" + subTree.parent(tree).label()+ "->" +leafs[0].toString().toLowerCase());
                }
                else if (subTree.isLeaf()){continue;} //the leaves were already addressed above
                else { //the node is a regular node
                    List<Tree> children = subTree.getChildrenAsList();
                    //StringBuilder string = new StringBuilder();
                    unlexProdRules.append(subTree.label()).append("->"); //prints the root note
                    unlexProdRulesWGrandparent.append(subTree.label()).append("^");
                    if(subTree.parent(tree) == null) { //the parent is the root
                        unlexProdRulesWGrandparent.append("R").append("->");
                    } else {
                        unlexProdRulesWGrandparent.append(subTree.parent(tree).label()).append("->");
                    }
                    Iterator<Tree> myChildren = children.iterator();
                    while (myChildren.hasNext()){
                        Tree myChild = myChildren.next();
                        unlexProdRules.append(myChild.label());
                        unlexProdRulesWGrandparent.append(myChild.label());
                        if (myChildren.hasNext()){
                            unlexProdRules.append("__");
                            unlexProdRulesWGrandparent.append("__");
                        } else {
                            unlexProdRules.append(" ");
                            unlexProdRulesWGrandparent.append(" ");
                        }
                    }
                }
            }

            DeceptionInstance myInstance = new DeceptionInstance(myRecord.getId(), myRecord.getMyClass());
            myInstance.setSentence(words.toString());
            myInstance.setPOS(pos.toString());
            myInstance.setLexProdRules(lexProdRules.toString());
            myInstance.setLexProdRulesWGrandparent(lexProdRulesWGrandparent.toString());
            myInstance.setUnlexProdRules(unlexProdRules.toString());
            myInstance.setUnlexProdRulesWGrandparent(unlexProdRulesWGrandparent.toString());
            instances.add(myInstance);

            logger.debug(myRecord.getText());
            logger.debug(words.toString());
            logger.debug(pos.toString());
            logger.debug("parse tree:\n" + tree.toString());
            logger.debug(unlexProdRules.toString());
            logger.debug(unlexProdRulesWGrandparent.toString());
            logger.debug(lexProdRules.toString());
            logger.debug(lexProdRulesWGrandparent.toString());
        }
        return instances;
    }
}
