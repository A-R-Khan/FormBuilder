package formbuilder.components;

import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 *
 * This class is responsible for arranging sections, questions, options etc of the form into an ordered format
 *
 *   Form is of the following structure, stored as a Map <String, Map <String, Map <String, String>>>
 *
 *   sections:
 *   ID --
 *       |--- position
 *       |--- title
 *       |--- description
 *       |
 *       |--- numQuestions
 *       |
 *       |--- sectionType
 *
 *   questions:
 *   ID --
 *       |--- sectionID
 *       |--- position
 *       |--- description
 *       |--- title
 *       |
 *       |--- questionType
 *       |--- responseType
 *       |--- isRequired
 *       |
 *       |--- hasOptions
 *       |--- numOptions
 *       |
 *       |--- validationType            |
 *       |--- validationCondition       |--- Optional validation parameters that you will need to 
 *       |--- validationCompareOperands |    implement youself in the abstract validation callback provided
 *       |--- validationErrorText       |
 *
 *   options:
 *   ID --
 *       |--- questionId
 *       |--- position
 *       |--- title
 *       |
 *       |--- responseType
 *
 *
 */

/*
    Generic parameters

    S : Enumeration containing type of sections
    Q : Enumeration containing type of questions    
    R : Enumeration containing types of responses
*/

public abstract class Form <S, Q, R> {

    // convenience enumerations to parse form
    private enum FormKey
    {
        KEY_FORM_SECTIONS_DATA ("sections"),
        KEY_FORM_QUESTIONS_DATA ("questions"),
        KEY_FORM_OPTIONS_DATA ("options"),

        KEY_SECTION_SECTION_TYPE ("sectionType"),
        KEY_SECTION_POSITION ("position"),
        KEY_SECTION_TITLE ("title"),
        KEY_SECTION_DESCRIPTION ("description"),
        KEY_SECTION_NUM_QUESTIONS ("numQuestions"),

        KEY_QUESTION_QUESTION_TYPE ("questionType"),
        KEY_QUESTION_RESPONSE_TYPE ("responseType"),
        KEY_QUESTION_SECTION_ID ("sectionID"),
        KEY_QUESTION_POSITION ("position"),
        KEY_QUESTION_TITLE ("title"),
        KEY_QUESTION_DESCRIPTION ("description"),
        KEY_QUESTION_IS_REQUIRED ("isRequired"),
        KEY_QUESTION_HAS_OPTIONS ("hasOptions"),
        KEY_QUESTION_NUM_OPTIONS ("numOptions"),

        KEY_OPTION_RESPONSE_TYPE ("responseType"),
        KEY_OPTION_QUESTION_ID ("questionID"),
        KEY_OPTION_POSITION ("position"),
        KEY_OPTION_TITLE ("title"),
        ;

        private String key;

        FormKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    // initialize this if needed
    private String formID;

    protected void setFormID(String ID) {
        formID = ID;
    }
    public String getFormID() {
        return formID;
    }

    // For giving completed form with responses back if needed
    private Map <String, Map <String, Map <String, String >>> formMap;

    // Map of IDs to FormElements for easy access if required
    protected Map<String, Section> formSections = new HashMap<>();
    protected Map<String, Question> formQuestions = new HashMap<>();
    protected Map<String, Option> formOptions = new HashMap<>();

    // sequential sections, ordered by position
    protected List<Section> sectionsList;


    // give a Map of the format shown below, which should be encoded in the format shown in the JavaDoc comment above this class
    public Form( @NonNull Map<String, Map<String, Map<String, String>>> formData) throws ParseException {

        Log.e("FORM", formData.toString());

        Map<String, Map<String, String>> sectionsData;
        Map<String, Map<String, String>> questionsData;
        Map<String, Map<String, String>> optionsData;

        sectionsData = formData.get(FormKey.KEY_FORM_SECTIONS_DATA.getKey());
        questionsData = formData.get(FormKey.KEY_FORM_QUESTIONS_DATA.getKey());
        optionsData = formData.get(FormKey.KEY_FORM_OPTIONS_DATA.getKey());


        /*
         A form cannot be without at least one section and question. Options can be null but only if
         no question requires options
        */
        if (sectionsData == null || questionsData == null) {
            throw new ParseException("Unable to parse form because null section or question", 0);
        }

        this.formMap = formData;

        int numSections = sectionsData.size();

        // initializing array for easy adding later
        sectionsList = new ArrayList<>(numSections);
        for (int i = 0; i < numSections; ++i) {
            sectionsList.add(null);
        }

        try {

            distributeSections(sectionsData);
            distributeQuestions(questionsData);
            distributeOptions(optionsData);

            // call the callbacks for each of the created elements in order, so that external actions
            // can be executed as and when needed

            for (Section createdSection : sectionsList) {
                if (createdSection == null) throw new NullPointerException();
                onSectionCreated(createdSection);

                for (Question createdQuestion : createdSection.getSectionQuestions()) {
                    if (createdQuestion == null) throw new NullPointerException();
                    onQuestionCreated(createdQuestion);

                    if (createdQuestion.getQuestionOptions() == null) continue;

                    for (Option createdOption : createdQuestion.getQuestionOptions()) {

                        if (createdOption == null) throw new NullPointerException();
                        onOptionCreated(createdOption);
                    }
                }
            }

        }
        catch (NullPointerException|ClassCastException e) {
            e.printStackTrace();
            throw new ParseException("Unable to parse form", 0);
        }
    }

    // parses the data and populates Sections into the sectionsList
    @SuppressWarnings("ConstantConditions")
    private void distributeSections(@NonNull Map <String, Map <String, String>> sectionsData) {

        for (Map.Entry<String, Map<String, String>> entry : sectionsData.entrySet()) {

            Map<String, String> sectionData = entry.getValue();

            Section section = new Section(entry.getKey(),
                    getSectionTypeFromString(
                            sectionData.get(FormKey.KEY_SECTION_SECTION_TYPE.getKey()
                            )
                    )
            );

            section.title = sectionData.get(
                    FormKey.KEY_SECTION_TITLE.getKey()
            );
            section.description = sectionData.get(
                    FormKey.KEY_SECTION_DESCRIPTION.getKey()
            );

            int numQuestions = Integer.parseInt(sectionData.get(
                    FormKey.KEY_SECTION_NUM_QUESTIONS.getKey())
            );
            section.sectionQuestions = new ArrayList<>(numQuestions);

            for (int i = 0; i < numQuestions; ++i) {
                section.sectionQuestions.add(null);
            }

            formSections.put(section.getID(), section);
            sectionsList.set(Integer.parseInt(
                    sectionData.get(FormKey.KEY_SECTION_POSITION.getKey())), section);
        }

    }

    // parses the data and populates Questions into questionsList 
    @SuppressWarnings("ConstantConditions")
    private void distributeQuestions(@NonNull Map <String, Map <String, String>> questionsData) {

        for (Map.Entry<String, Map<String, String>> entry : questionsData.entrySet()) {

            Map<String, String> questionData = entry.getValue();

            Question question = new Question(entry.getKey(), getQuestionTypeFromString(questionData.get(
                    FormKey.KEY_QUESTION_QUESTION_TYPE.getKey()))
            );

            question.sectionID = questionData.get(FormKey.KEY_QUESTION_SECTION_ID.getKey());

            question.title = questionData.get(
                    FormKey.KEY_QUESTION_TITLE.getKey()
            );
            question.description = questionData.get(
                    FormKey.KEY_QUESTION_DESCRIPTION.getKey()
            );
            question.isRequired = Boolean.parseBoolean(questionData.get(
                    FormKey.KEY_QUESTION_IS_REQUIRED.getKey())
            );

            question.setResponseType(getResponseTypeFromString(questionData.get(
                    FormKey.KEY_QUESTION_RESPONSE_TYPE.getKey()))
            );

            int position = Integer.parseInt(questionData.get(
                    FormKey.KEY_QUESTION_POSITION.getKey())
            );
            formSections.get(questionData.get(
                    FormKey.KEY_QUESTION_SECTION_ID.getKey()))
                    .getSectionQuestions()
                    .set(position, question);

            formQuestions.put(question.getID(), question);

            boolean hasOptions = Boolean.parseBoolean(questionData.get(
                    FormKey.KEY_QUESTION_HAS_OPTIONS.getKey())
            );
            if (hasOptions) {
                int numOptions = Integer.parseInt(questionData.get(
                        FormKey.KEY_QUESTION_NUM_OPTIONS.getKey())
                );
                question.questionOptions = new ArrayList<>(numOptions);

                for (int i = 0; i < numOptions; ++i) {
                    question.getQuestionOptions().add(null);
                }
            }
        }

    }

    // parses data and populates Options into OptionsList
    @SuppressWarnings("ConstantConditions")
    private void distributeOptions(@Nullable Map <String, Map <String, String>> optionsData) {

        // if it is null then check if any question has options
        if (optionsData == null) {
            return;
        }

        for (Map.Entry<String, Map<String, String>> entry : optionsData.entrySet()) {

            Map<String, String> optionData = entry.getValue();

            Option option = new Option(entry.getKey(), getResponseTypeFromString(optionData.get(
                    FormKey.KEY_OPTION_RESPONSE_TYPE.getKey()))
            );

            option.title = optionData.get(
                    FormKey.KEY_OPTION_TITLE.getKey()
            );

            int position = Integer.parseInt(optionData.get(
                    FormKey.KEY_OPTION_POSITION.getKey())
            );
            formQuestions.get(optionData.get(
                    FormKey.KEY_OPTION_QUESTION_ID.getKey()))
                    .getQuestionOptions()
                    .set(position, option);

            formOptions.put(option.getID(), option);
        }

    }
    
    // is to be used when you want to retrieve the responses or if you want to make custom changes
    public Map<String, Map<String, Map<String, String>>> getFormMap() {
        return formMap;
    }

    //TODO write validation code
    public abstract boolean validateFormResponses();


    /*
     use these to do whatever you want with the form elements that were created. These methods are called after parse is complete
     set callbacks, inflate views, etc with them

     Use these after initializing form as an anonymous inner class or named non-static inner class
    */
    protected abstract void onSectionCreated(Section section);
    protected abstract void onQuestionCreated(Question question);
    protected abstract void onOptionCreated(Option option);

    protected abstract S getSectionTypeFromString(String sectionType);
    protected abstract Q getQuestionTypeFromString(String questionType);
    protected abstract R getResponseTypeFromString(String responseType);

    /*
        ENSURE THESE CLASSES ARE NEVER USED OUTSIDE THIS CLASS EVER OR IT IS A
        ONE WAY TICKET TO MEMORY LEAK LAND
    */

    private static class FormElement<T> {

        public FormElement(String elementID, T elementType) {
            this.elementType = elementType;
            this.elementID = elementID;
        }

        private String elementID;
        private T elementType;

        protected String getID() {
            return elementID;
        }

        public T getType() {
            return elementType;
        }
    }

    class Section extends FormElement<S> {

        private String title;
        private String description;

        private List<Question> sectionQuestions;

        public Section(String elementID, S elementType) {
            super(elementID, elementType);
        }

        public List<Question> getSectionQuestions() {
            return sectionQuestions;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }


    class Question extends FormElement<Q> {

        private String sectionID;

        private String title;
        private String description;

        private R responseType;
        private String response;

        private boolean isRequired;

        private List<Option> questionOptions;

        public Question(String elementID, Q elementType) {
            super(elementID, elementType);
        }

        public boolean getIsRequired() {
            return isRequired;
        }

        public String getResponse(){
            return response;
        }

        public void setResponse(String response){
            this.response = response;
        }

        public List<Option> getQuestionOptions() {
            return questionOptions;
        }

        public R getResponseType() {
            return responseType;
        }

        public void setResponseType(R responseType) {
            this.responseType = responseType;
        }

        public String getSectionID() {
            return sectionID;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public void setSectionID(String sectionID) {
            this.sectionID = sectionID;
        }
    }


    class Option extends FormElement<R> {

        private String questionID;
        private String title;

        private String response;

        public Option(String elementID, R elementType) {
            super(elementID, elementType);
        }

        public String getResponse() {
            return this.response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }

}
