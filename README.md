# DeceptionDetection
IntelliJ Project using Maven. POM file is included.
Compiles using JAVA 1.8 (required by Stanford CoreNLP - a dependency).

Note:
The dataset (in csv format) has some commas inside the various fields that the CSVParser is unable to properly process. The fields preceeding the text field need to be preprocessed to remove all the commas. The DeceptionDetector code addresses the commas inside the text field.
For example:
id,_gender,age,education,country,text,class
2_f_l_1,Female,22,'Some college, no degree',USA,'I can bench press 600 pounds, anywhere.',lie
Can be reformatted as (note the "," replaced with "-" in the education field):
2_f_l_1,Female,22,'Some college - no degree',USA,'I can bench press 600 pounds, anywhere.',lie
Despite the comma remaining in the text field, the algorithm will be able to account for it.
