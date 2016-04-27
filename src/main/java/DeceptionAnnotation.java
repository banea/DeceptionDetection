/**
 * Created by carmennb on 4/21/16.
 */
public class DeceptionAnnotation {
    private String id;
    private String gender;
    private int age;
    private String education;
    private String country;
    private String text;
    private Classification myClass;
    public enum Classification {truth,lie};

    public DeceptionAnnotation(String id, String gender, int age, String education, String country,
                               String text, String label ){
        this.id = id;
        this.gender = gender;
        this.age = age;
        this.education = education;
        this.country = country;
        this.text = text;
        if (label.equals("truth")){
            this.myClass =  Classification.truth;
        } else if (label.equals("lie")){
            this.myClass = Classification.lie;
        }
    }

    @Override
    public String toString() {
        return "DeceptionAnnotation{" +
                "id='" + id + '\'' +
                ", gender='" + gender + '\'' +
                ", age=" + age +
                ", education='" + education + '\'' +
                ", country='" + country + '\'' +
                ", text='" + text + '\'' +
                ", myClass=" + myClass +
                '}';
    }

    public String getId (){return this.id;}
    public String getText (){return this.text;}
    public Classification getMyClass(){return  this.myClass;}
}
