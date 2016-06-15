/**
 * Created by carmennb on 6/15/16.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // if no parameters passed, provide usage help
        if (args.length != 0) {
            if (args[0].matches("build-dataset") && args.length == 3) {
                //two additional parameters should be provided: input and output file
                DeceptionDatasetBuilder.main(args);
            } else if (args[0].matches("classify") && (args.length == 2 || args.length == 3)) {
                //one additional parameter should be provided: input arff file
                DeceptionClassifier.main(args);
            } else {
                printUsageExample();
            }
        } else {
            printUsageExample();
        }
    }

    public static void printUsageExample(){
        System.out.println("\nIncorrect usage: need to pass one of the following options:");
        System.out.println("\tjava -jar deception-detector.jar build-dataset <input_filename.csv> <output_filename.arff>");
        System.out.println("\tjava -jar deception-detector.jar classify <input_filename.arff> [\"<attributes_to_remove>\"]\n");
    }
}
