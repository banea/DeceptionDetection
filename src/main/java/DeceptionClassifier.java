import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by carmennb@umich.edu on 4/26/16.
 */

public class DeceptionClassifier {
    static Logger logger;

    public class Tuple{
        public Double TP;
        public Double TN;
        public Double FP;
        public Double FN;

        public Tuple(Double TP, Double FN, Double FP, Double TN){
            this.TP = TP;
            this.FN = FN;
            this.FP = FP;
            this.TN = TN;
        }
    }

    public static void main(String[] args) throws Exception{

        //check if the arff file exists; if not, terminate.
        String arffInputFile = args[1];
        File f = new File(arffInputFile);
        if (!f.exists() || f.isDirectory()){
            System.out.println("Input file not found!");
            System.exit(0);
        }

        //load the attributes that should be removed
        String[] attributesToRemove = null;
        if (args.length == 3) {
            attributesToRemove = args[2].split(" ");
        }

        /** Initialize logger instance */
        logger = LoggerFactory.getLogger(new Object() {
        }.getClass());
        try {
            /** The log config file path. */
            String logConfigFilePath = "src/main/resources/log4j.file.properties";
            DataInputStream rf = new DataInputStream(new FileInputStream(logConfigFilePath));
            PropertyConfigurator.configure(rf);
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        DeceptionClassifier myClassifier = new DeceptionClassifier();
        logger.info("Reading file " + arffInputFile + "...");
        //load the dataset
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(arffInputFile);
        Instances data = source.getDataSet();
        logger.debug("Original: \n" + data.lastInstance().toString());

        //set class attribute
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);

        //generate a mapping of the attributes, so that they can be addressed by name, instead of key
        logger.info("");
        logger.info("Original attribute list:");
        myClassifier.listAttributes(data);
        Map<String, Integer> myMapping = new HashMap<String, Integer>();
        Enumeration<Attribute> myEnum  = data.enumerateAttributes();
        while(myEnum.hasMoreElements()) {
            Attribute myAttribute = myEnum.nextElement();
            myMapping.put(myAttribute.name(), myAttribute.index()+1);
        }

        //remove some attributes; after this the prior mapping needs to be readjusted
        Remove remove = new Remove();
        //the "ID" attribute should NEVER be removed here, as it is used in generating the folds in cross-validation
        //sets which attributes should be removed from the mix. The values commented are staying, those uncommented are
        //discarded.
        if (attributesToRemove != null){
            ArrayList<Integer> indeces = new ArrayList<Integer>();
            for (int i = 0; i < attributesToRemove.length; i++){
                if (myMapping.containsKey(attributesToRemove[i]) && !attributesToRemove[i].matches("ID")){
                    indeces.add(myMapping.get(attributesToRemove[i]));
                }
            }
            Collections.sort(indeces);
            String attributeIndeces = "";
            for (Integer index: indeces) {
                attributeIndeces+=index + ",";
            }

            if(attributeIndeces.length() > 0) {
                //remove the last comma
                attributeIndeces = attributeIndeces.substring(0, attributeIndeces.length() - 1);
                logger.info("Removing attributes with indeces: " + attributeIndeces);
                remove.setAttributeIndices(attributeIndeces);
                remove.setInputFormat(data);
                data = Filter.useFilter(data, remove);
            }
        }

        logger.info("");
        logger.info("Attribute list after attribute removal:");
        myClassifier.listAttributes(data);
        //set which attributes to be processed with the various filters
        Enumeration<Attribute> myE  = data.enumerateAttributes();
        List<Integer> productionRuleAttributes = new ArrayList<Integer>();
        List<Integer> textAttributes = new ArrayList<Integer>();
        List<Integer> posAttributes = new ArrayList<Integer>();
        myMapping.clear();
        while(myE.hasMoreElements()) {
            Attribute myAttribute = myE.nextElement();
            //readjust the indexing (after attrib removal)
            myMapping.put(myAttribute.name(), myAttribute.index()+1);
            //populate the production rules, pos and text type attributes
            if (myAttribute.name().contains("ProdRules")){
                productionRuleAttributes.add(myAttribute.index()+1);
            } else if (myAttribute.name().contains("POS")){
                posAttributes.add(myAttribute.index()+1);
            } else if (myAttribute.name().contains("sentence")){
                textAttributes.add(myAttribute.index()+1);
            }
        }

        //get a string representation of the attribute indeces that need to be processed
        String productionRuleAttribIndeces = myClassifier.getFilterAttribIndeces(productionRuleAttributes);
        String posAttributesIndeces = myClassifier.getFilterAttribIndeces(posAttributes);
        String textAttributesIndeces = myClassifier.getFilterAttribIndeces(textAttributes);

        //initialize tokenizer
        WordTokenizer spaceWT = new WordTokenizer();
        spaceWT.setDelimiters(" ");

        //process production rules
        if (productionRuleAttributes.size() > 0) {
            StringToWordVector productionRuleFilter = new StringToWordVector();
            productionRuleFilter.setInputFormat(data);
            productionRuleFilter.setTokenizer(spaceWT);
            productionRuleFilter.setOptions(weka.core.Utils.splitOptions("-R \""+productionRuleAttribIndeces+"\" -P _ -W 5000 -prune-rate -1.0 -C -N 0 -stemmer weka.core.stemmers.NullStemmer -M 2 "));
            data = Filter.useFilter(data, productionRuleFilter);
            logger.debug("After production rules filtering: \n" + data.lastInstance().toString());
        }

        //process pos rules
        if (posAttributes.size() > 0){
            StringToWordVector posFilter = new StringToWordVector();
            posFilter.setInputFormat(data);
            posFilter.setTokenizer(spaceWT);
            posFilter.setOptions(weka.core.Utils.splitOptions("-R \"" + posAttributesIndeces + "\" -P + -W 5000 -prune-rate -1.0 -C -N 0 -stemmer weka.core.stemmers.NullStemmer -M 2 "));
            data = Filter.useFilter(data,posFilter);
            logger.debug("After pos filtering: \n" + data.lastInstance().toString());
        }

        //process text
        if (textAttributes.size() > 0) {
            StringToWordVector wordFilter = new StringToWordVector();
            wordFilter.setTokenizer(spaceWT);
            wordFilter.setOptions(weka.core.Utils.splitOptions("-R \""+ textAttributesIndeces +"\" -P ! -W 10000 -prune-rate -1.0 -C -L -N 0 -stemmer weka.core.stemmers.NullStemmer -M 2 "));
            wordFilter.setInputFormat(data);
            data = Filter.useFilter(data, wordFilter);
            logger.debug("After word filter: \n" + data.lastInstance().toString());
        }

        //evaluate a classifier on the data (while splitting the data into test/train while
        //making sure that all instances from a given user are either in train or test, but
        //not both
        myClassifier.generateSplitsAndValidate(5, data);

        //evaluate a classifier on the data, but does not keep track of whether same user
        //instances appear in both test and train
        //myClassifier.crossValidation(5, data);

    }


    public void generateSplitsAndValidate  (int nFolds, Instances dataset) throws Exception {
        double correct = 0;
        double total = 0;
        List<Tuple> evaluationMatrix = new ArrayList<Tuple>();

        //randomize the data
        Instances randData = new Instances(dataset);
        Random random = new Random(1);
        randData.randomize(random);

        //load user ids from the data
        Map<String,Integer> userMapping = new HashMap<String,Integer>();
        int userCount = 0;
        Iterator<Instance> itr = randData.iterator();
        while (itr.hasNext()){
            Instance myInstance = itr.next();
            // id in the file is of type "183_f_l"; the first two positions are the user id,
            // the last position is the class. We ignore the class.
            String rawID = myInstance.stringValue(0);
            String userID = rawID.substring(0,rawID.lastIndexOf("_"));

            //if the user has not yet been added to the mapping
            //add it and set its test fold id
            if (userMapping.containsKey(userID) == false){
                userCount++;
                userMapping.put(userID, userCount % nFolds + 1);
            }
        }

        //print header
        logger.info("");
        logger.info("Fold\t#Test-inst.\t#Train-inst.\tTP\tFN\tFP\tTN");
        //split into test and train, making sure that ids reserved for test do not appear in train
        for (Integer i = 1; i <= nFolds; i++){
            //instantiate empty test set, with a maximum size of 1/4 of the entire set
            Instances test = new Instances(randData, randData.size()/4);
            //instantiate empty train set, with a maximum size of the entire set
            Instances train = new Instances(randData, randData.size());
            for (Instance myInstance : randData){
                //get the userID value
                String rawID = myInstance.stringValue(0);
                String userID = rawID.substring(0,rawID.lastIndexOf("_"));
                //if user mapping matches the test fold, add instance to test set
                if (userMapping.get(userID) == i) {
                    test.add(myInstance);
                } else {        //else, add it to train set
                    train.add(myInstance);
                }
            }

            //delete userID attribute
            test.deleteAttributeAt(0);
            train.deleteAttributeAt(0);

            logger.info(" " + train.classAttribute().toString());
            weka.classifiers.functions.SMO scheme = new weka.classifiers.functions.SMO();
            scheme.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));
            //MultilayerPerceptron cls = new MultilayerPerceptron();
            //cls.setOptions(weka.core.Utils.splitOptions("-L 0.3 -M 0.2 -N 500 -V 0 -S 0 -E 20 -H a"));
            scheme.buildClassifier(train);
            Evaluation eval = new Evaluation(train);
            eval.evaluateModel(scheme, test);

            //logger.info(eval.toSummaryString());

            correct+=eval.correct();
            total+=test.numInstances();
            evaluationMatrix.add(new Tuple(eval.numTruePositives(0), eval.numFalseNegatives(0),
                    eval.numFalsePositives(0),  eval.numTrueNegatives(0)));
            logger.info(i +"\t" + test.numInstances() + "\t" + train.numInstances() +"\t" + eval.numTruePositives(0) + "\t" +
                    eval.numFalseNegatives(0) + "\t" + eval.numFalsePositives(0) + "\t" + eval.numTrueNegatives(0));
        }
        double accuracy = correct/total * 100;

        double tpTotal = 0;
        double fnTotal = 0;
        double fpTotal = 0;
        double tnTotal = 0;

        for (Tuple evaluationInstance: evaluationMatrix){
            tpTotal += evaluationInstance.TP;
            fnTotal += evaluationInstance.FN;
            fpTotal += evaluationInstance.FP;
            tnTotal += evaluationInstance.TN;
        }

        double precPos = tpTotal / (tpTotal + fpTotal) * 100;
        double recallPos = tpTotal / (tpTotal + fnTotal) * 100;
        double fPos = 2 * precPos * recallPos / (precPos + recallPos);
        double precNon = tnTotal / (tnTotal + fnTotal) * 100;
        double recallNon = tnTotal / (tnTotal + fpTotal) * 100;
        double fNon = 2 * precNon * recallNon / (precNon + recallNon);

        //print overall evaluation header
        logger.info("P_pos\tR_pos\tF_pos\tP_non\tR_non\tF_non\tAcc");

        //logger.info("P_pos\t" + precPos +"\tR_pos\t" + recallPos + "\tP_non\t" + precNon + "\tR_non\t" + recallNon);
        //logger.info("Correct\t" + correct + "\tTotal\t" + total + "\tAccuracy\t" + accuracy);

        logger.info(String.format("%.2f", precPos) + "\t" + String.format("%.2f", recallPos) + "\t" + String.format("%.2f", fPos) +
                "\t" + String.format("%.2f", precNon) + "\t" + String.format("%.2f", recallNon) + "\t" + String.format("%.2f", fNon) +
                "\t" + String.format("%.2f",accuracy));
    }


    public void crossValidation (int nFolds, Instances dataset) throws Exception {
        double correct = 0;
        double total = 0;

        List<Tuple> evaluationMatrix = new ArrayList<Tuple>();

        //remove "ID" attribute;
        dataset.deleteAttributeAt(0);

        Instances randData = new Instances(dataset);
        Random random = new Random(1);
        randData.randomize(random);
        randData.stratify(nFolds);

        //print header
        logger.info("Fold\t#Test-instances\t#Train-instances\tTP\tFN\tFP\tTN");
       for (int n = 0; n < nFolds; n++){
           Instances train = randData.trainCV(nFolds, n);
           Instances test = randData.testCV(nFolds, n);

           weka.classifiers.functions.SMO scheme = new weka.classifiers.functions.SMO();
           scheme.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));
           //MultilayerPerceptron cls = new MultilayerPerceptron();
           //cls.setOptions(weka.core.Utils.splitOptions("-L 0.3 -M 0.2 -N 500 -V 0 -S 0 -E 20 -H a"));
           scheme.buildClassifier(train);
           Evaluation eval = new Evaluation(train);
           eval.evaluateModel(scheme, test);

           logger.info(eval.toSummaryString());

           correct+=eval.correct();
           total+=test.numInstances();
           evaluationMatrix.add(new Tuple(eval.numTruePositives(0), eval.numFalseNegatives(0),
                   eval.numFalsePositives(0),  eval.numTrueNegatives(0)));
           //print per fold truth table
           logger.info(n +"\t" + test.numInstances() + "\t" + train.numInstances() +"\t" + eval.numTruePositives(0) + "\t" +
                   eval.numFalseNegatives(0) + "\t" + eval.numFalsePositives(0) + "\t" + eval.numTrueNegatives(0));
        }
        double accuracy = correct/total * 100;

        double tpTotal = 0;
        double fnTotal = 0;
        double fpTotal = 0;
        double tnTotal = 0;

        for (Tuple evaluationInstance: evaluationMatrix){
            tpTotal += evaluationInstance.TP;
            fnTotal += evaluationInstance.FN;
            fpTotal += evaluationInstance.FP;
            tnTotal += evaluationInstance.TN;
        }

        double precPos = tpTotal / (tpTotal + fpTotal) * 100;
        double recallPos = tpTotal / (tpTotal + fnTotal) * 100;
        double fPos = 2 * precPos * recallPos / (precPos + recallPos);
        double precNon = tnTotal / (tnTotal + fnTotal) * 100;
        double recallNon = tnTotal / (tnTotal + fpTotal) * 100;
        double fNon = 2 * precNon * recallNon / (precNon + recallNon);

        //print overall evaluation header
        logger.info("P_pos\tR_pos\tF_pos\tP_non\tR_non\tF_non\tAcc");

        //logger.info("P_pos\t" + precPos +"\tR_pos\t" + recallPos + "\tP_non\t" + precNon + "\tR_non\t" + recallNon);
        //logger.info("Correct\t" + correct + "\tTotal\t" + total + "\tAccuracy\t" + accuracy);

        logger.info(String.format("%.2f", precPos) + "\t" + String.format("%.2f", recallPos) + "\t" + String.format("%.2f", fPos) +
                "\t" + String.format("%.2f", precNon) + "\t" + String.format("%.2f", recallNon) + "\t" + String.format("%.2f", fNon) +
                "\t" + String.format("%.2f",accuracy));
    }

    public void listAttributes(Instances data){
        Enumeration<Attribute> myEnum  = data.enumerateAttributes();
        while(myEnum.hasMoreElements()) {
            Attribute myAttribute = myEnum.nextElement();
            logger.info(myAttribute.index() + " " + myAttribute.toString());
        }
    }

    public String getFilterAttribIndeces (List<Integer> myList) {
        StringBuilder filterAttribIndeces = new StringBuilder();
        Iterator myItr = myList.iterator();
        while (myItr.hasNext()) {
            filterAttribIndeces.append(myItr.next());
            if (myItr.hasNext()) {
                filterAttribIndeces.append(",");
            }
        }
        logger.debug(filterAttribIndeces.toString());
        return filterAttribIndeces.toString();
    }
}
