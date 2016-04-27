import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by carmennb on 4/26/16.
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

        DeceptionClassifier myClassifier = new DeceptionClassifier();
        String filepath = "output/overall.arff";
        logger.info("Reading file " + filepath + "...");
        //load the dataset
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(filepath);
        Instances data = source.getDataSet();
        logger.debug("Original: \n" + data.lastInstance().toString());

        //set class attribute
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);

        myClassifier.listAttributes(data);
        Map<String, Integer> myMapping = new HashMap<String, Integer>();
        Enumeration<Attribute> myEnum  = data.enumerateAttributes();
        while(myEnum.hasMoreElements()) {
            Attribute myAttribute = myEnum.nextElement();
            myMapping.put(myAttribute.name(), myAttribute.index()+1);
        }

        //remove some attributes
        Remove remove = new Remove();
        remove.setAttributeIndices(myMapping.get("ID").toString());
        remove.setInputFormat(data);
        data = Filter.useFilter(data, remove);

        //set which attributes to be processed with the various filters
        Enumeration<Attribute> myE  = data.enumerateAttributes();
        List<Integer> productionRuleAttributes = new ArrayList<Integer>();
        List<Integer> textAttributes = new ArrayList<Integer>();
        List<Integer> posAttributes = new ArrayList<Integer>();
        while(myE.hasMoreElements()) {
            Attribute myAttribute = myE.nextElement();
            if (myAttribute.name().contains("ProdRules")){
                productionRuleAttributes.add(myAttribute.index()+1);
            } else if (myAttribute.name().contains("POS")){
                posAttributes.add(myAttribute.index()+1);
            } else if (myAttribute.name().contains("sentence")){
                textAttributes.add(myAttribute.index()+1);
            }
        }

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




        /*
        //use weka to split POS
        //in position 3
        StringToWordVector posFilter = new StringToWordVector();
        posFilter.setInputFormat(afterProductionRuleFilter);
        //Make a tokenizer
        posFilter.setTokenizer(spaceWT);
        posFilter.setOptions(weka.core.Utils.splitOptions("-R \"3\" -P + -W 5000 -prune-rate -1.0 -C -N 0 -stemmer weka.core.stemmers.NullStemmer -M 2 "));

        Instances afterPOSFilter = Filter.useFilter(afterProductionRuleFilter, posFilter);
        logger.debug("After pos filtering: \n" + afterPOSFilter.lastInstance().toString());
        //logger.debug(dataFiltered.toString());

        //use weka to split strings into words for the original dataset
        //in position 2 -> unigrams
        StringToWordVector wordFilter = new StringToWordVector();
        wordFilter.setTokenizer(spaceWT);
        wordFilter.setOptions(weka.core.Utils.splitOptions("-R \"2\" -P ! -W 10000 -prune-rate -1.0 -C -L -N 0 -stemmer weka.core.stemmers.NullStemmer -M 2 "));
        wordFilter.setInputFormat(afterPOSFilter);
        Instances afterWordFilter = Filter.useFilter(afterPOSFilter, wordFilter);
        logger.debug("After word filter: \n" + afterWordFilter.lastInstance().toString());
        */

        //after production rules, class index is 3
        /*
        logger.debug(afterProductionRuleFilter.lastInstance().toString());
        logger.debug("Class index: "+ afterProductionRuleFilter.lastInstance().classIndex());
        afterProductionRuleFilter.deleteAttributeAt(2); //delete pos attribute
        logger.debug(afterProductionRuleFilter.lastInstance().toString());
        afterProductionRuleFilter.deleteAttributeAt(1); //delete sentence
        logger.debug(afterProductionRuleFilter.lastInstance().toString());
        afterProductionRuleFilter.deleteAttributeAt(0); //delete id
        logger.debug(afterProductionRuleFilter.lastInstance().toString());
        */

        myClassifier.crossValidation(5, data);
    }


    public void crossValidation (int nFolds, Instances dataset) throws Exception {
        double correct = 0;
        double total = 0;

        //dataset.setClassIndex(13);
        List<Tuple> evaluationMatrix = new ArrayList<Tuple>();

        Instances randData = new Instances(dataset);
        Random random = new Random(1);
        randData.randomize(random);
        randData.stratify(nFolds);

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
           logger.info(n +"\t" + test.numInstances() + "\t" + train.numInstances() +"\t" + eval.numTruePositives(0) + "\t" +
                   eval.numFalseNegatives(0) + "\t" + eval.numFalsePositives(0) + "\t" + eval.numTrueNegatives(0));
        }
        double accuracy = correct/total;

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

        double precPos = tpTotal / (tpTotal + fpTotal);
        double recallPos = tpTotal / (tpTotal + fnTotal);
        double precNon = tnTotal / (tnTotal + fnTotal);
        double recallNon = tnTotal / (tnTotal + fpTotal);
        logger.info("P_pos\t" + precPos +"\tR_pos\t" + recallPos + "\tP_non\t" + precNon + "\tR_non\t" + recallNon);
        logger.info("Correct\t" + correct + "\tTotal\t" + total + "\tAccuracy\t" + accuracy);
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
