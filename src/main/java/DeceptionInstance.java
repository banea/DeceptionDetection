/**
 * Created by carmennb on 4/21/16.
 */
public class DeceptionInstance {
    private String uniqueIdentifier;
    private String sentence;
    private String POS;
    private String dependencies;
    private String constituencies;
    private String unlexProdRules;
    private String lexProdRules;
    private String unlexProdRulesWGrandparent;
    private String lexProdRulesWGrandparent;
    private DeceptionAnnotation.Classification myClass;
    //public enum Classification {truth,lie};

    public DeceptionInstance(String uniqueIdentifier, DeceptionAnnotation.Classification myClass) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.myClass = myClass;
    }

    @Override
    public String toString() {
        return "DeceptionInstance{" +
                "uniqueIdentifier='" + uniqueIdentifier + '\'' +
                ", sentence='" + sentence + '\'' +
                ", POS='" + POS + '\'' +
                ", dependencies='" + dependencies + '\'' +
                ", constituencies='" + constituencies + '\'' +
                ", unlexProdRules='" + unlexProdRules + '\'' +
                ", lexProdRules='" + lexProdRules + '\'' +
                ", unlexProdRulesWGrandparent='" + unlexProdRulesWGrandparent + '\'' +
                ", lexProdRulesWGrandparent='" + lexProdRulesWGrandparent + '\'' +
                ", myClass=" + myClass +
                '}';
    }

    public static String getArffHeader(){
        StringBuilder header = new StringBuilder();
        header.append("@RELATION deception\n\n");
        header.append("@ATTRIBUTE ID string\n");
        header.append("@ATTRIBUTE sentence string\n");
        header.append("@ATTRIBUTE POS string\n");
        header.append("@ATTRIBUTE unlexProdRules string\n");
        header.append("@ATTRIBUTE unlexProdRulesWGrandparent string\n");
        header.append("@ATTRIBUTE lexProdRules string\n");
        header.append("@ATTRIBUTE lexProdRulesWGrandparent string\n");
        header.append("@ATTRIBUTE class {truth,lie}\n\n");
        header.append("@DATA\n");
        return header.toString();
    }

    public String getArffString() {
        StringBuilder arff = new StringBuilder();
        //ID
        arff.append('"').append(uniqueIdentifier).append("\", \"");
        //sentence
        arff.append(sentence).append("\", \"");
        //POS
        arff.append(POS).append("\", \"");
        //unlexProdRules
        arff.append(unlexProdRules).append("\", \"");
        //unlexProdRulesWGrandparent
        arff.append(unlexProdRulesWGrandparent).append("\", \"");
        //lexProdRules
        arff.append(lexProdRules).append("\", \"");
        //lexProdRulesWGrandparent
        arff.append(lexProdRulesWGrandparent).append("\", ");
        //class
        arff.append(myClass.toString());

        return arff.toString();
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getPOS() {
        return POS;
    }

    public void setPOS(String POS) {
        this.POS = POS;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public String getConstituencies() {
        return constituencies;
    }

    public void setConstituencies(String constituencies) {
        this.constituencies = constituencies;
    }

    public String getUnlexProdRules() {
        return unlexProdRules;
    }

    public void setUnlexProdRules(String unlexProdRules) {
        this.unlexProdRules = unlexProdRules;
    }

    public String getLexProdRules() {
        return lexProdRules;
    }

    public void setLexProdRules(String lexProdRules) {
        this.lexProdRules = lexProdRules;
    }

    public String getUnlexProdRulesWGrandparent() {
        return unlexProdRulesWGrandparent;
    }

    public void setUnlexProdRulesWGrandparent(String unlexProdRulesWGrandparent) {
        this.unlexProdRulesWGrandparent = unlexProdRulesWGrandparent;
    }

    public String getLexProdRulesWGrandparent() {
        return lexProdRulesWGrandparent;
    }

    public void setLexProdRulesWGrandparent(String lexProdRulesWGrandparent) {
        this.lexProdRulesWGrandparent = lexProdRulesWGrandparent;
    }

    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    public DeceptionAnnotation.Classification getMyClass() {
        return myClass;
    }
}
