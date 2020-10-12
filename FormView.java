package formbuilder.components;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.omada.junction.R;
import com.omada.junction.data.models.EventModel;
import com.omada.junction.viewmodels.content.EventViewHandler;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/*
*   This class uses a form that was already parsed using Form and inflates a form view based on that form, by creating form components, attaching
*   listeners, etc to the callbacks of a non static inner class that extends Form
*
 */

public class FormView extends FrameLayout {

    // DO NOT GET A REFERENCE TO FORM INSTANCE FROM OUTSIDE THIS CLASS
    private Form<SectionType, QuestionType, ResponseType> form;

    // Maps to store Views hashed by their corresponding FormElement IDs
    private Map <String, View> formSectionViews = new HashMap<>();
    private Map <String, View> formQuestionViews = new HashMap<>();
    private Map <String, View> formOptionViews = new HashMap<>();

    private EventModel eventModel;

    public FormView(Context context) {
        super(context);
    }

    public FormView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FormView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setForm() {

        try {

            // Creation of form class itself handles all the View inflation, etc just define your logic in the callbacks provided
            this.form = new RegistrationForm(eventModel.getEventForm());

            // Keeping the first View to be the first Section
            addView(
                    formSectionViews.get(
                            form.sectionsList.get(0).getID()
                    )
            );

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /*
    These inflate methods are used to inflate views, attach listeners where needed and return the resulting view.
    They are called in the callbacks implemented in RegistrationForm class (see below)
    
    They can be anything you want them to be. They are just to modularize the code. You can put them all in the 
    callbacks if you want to
    */

    private View inflateSectionView() {
        // refer to example usage below
        return null;
    }

    private View inflateQuestionView() {
        // refer to example usage below
        return null;
    }

    // EXAMPLE USAGE
    private View inflateResponseView() {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;

        // second argument is to get the View associated with the response that this question is for
        View v = inflater.inflate(R.layout.your_response_layout, (ViewGroup) formSectionViews.get(questionID), false);

        TextInputEditText shortTextInput = v.findViewById(R.id.your_text_input);

        shortTextInput
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                        //change response here so that you can use it later to send responses back when submitted
                        form.formQuestions.get(questionID).setResponse(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

        return v;
    }


    /*
    These enumerations are to hold the types of sections, questions and responses you will have
    eg: ResponseType can have RESPONSE_TYPE_DATE and QuestionType can have QUESTION_TYPE_YELLOW and QUESTION_TYPE_RED

    These enumerations are automatically given as a parameter in the callbacks so you can use them to inflate a different view if needed
    */
    private enum SectionType {
        SECTION_TYPE_DEFAULT
    }

    private enum QuestionType {
        QUESTION_TYPE_DEFAULT
    }

    private enum ResponseType {
        RESPONSE_TYPE_DEFAULT
    }

    /*
    This is a non-static class because it needs access to views and inflaters, etc from the FormView class in its callbacks
    Just remember to never leak this reference outside this scope or the View might not be garbage collected
    */

    private class RegistrationForm extends Form <SectionType, QuestionType, ResponseType> {

        public RegistrationForm(@NonNull Map<String, Map<String, Map<String, String>>> formData) throws ParseException {
            super(formData);
        }

        /*
        PLEASE NOTE THAT GOING TO THE NEXT SECTION, EXITING THE FORM, ETC ARE NOT IMPLEMENTED BUT YOU CAN SET CUSTOM
        KEYS IN THE ENCODED FORM IF NEEDED AND USE THE CALLBACKS TO SET LISTENERS TO CARRY OUT YOUR CUSTOM FUNCTIONS.
        THE DECISION TO DO THIS WAS DELIBERATE, IN THE INTEREST OF GENERALITY.

        REMEMBER THAT BY THE TIME THESE CALLBACKS ARE INVOKED, ALL THE FORM ELEMENTS HAVE BEEN INITIALIZED AND PUT INTO MAPS,
        HASHED BY THEIR ID, TO MAKE IT EASIER FOR THE USER TO IMPLEMENT EVEN THE MOST COMPLEX FUNCTIONALITY.
        */

        // create view for section here and store it in the map
        @Override
        protected void onSectionCreated(Section section) {

            // inflating section's View here
            View inflatedSectionView = inflateSectionView();
            
            /*
            ..... Do something with View.......
            */

            //  adding it to the Map for later retrieval
            formSectionViews.put(section.getID(), inflatedSectionView);
        }

        // create view for question here and store it in the map
        @Override
        protected void onQuestionCreated(Question question) {

            // Add main question view to section view from here
            View inflatedQuestionView = inflateQuestionView();
            ((MaterialTextView)inflatedQuestionView.findViewById(R.id.question_title_text)).setText(question.getTitle());
            ((MaterialTextView)inflatedQuestionView.findViewById(R.id.question_description_text)).setText(question.getDescription());

            formQuestionViews.put(question.getID(), inflatedQuestionView);

            /*
            Add response views that don't need any options from here and if required prepare the Question's View 
            to accept a certain type of response
            */
            switch (question.getResponseType()) {
                case RESPONSE_TYPE_DEFAULT:
                    break;
                case RESPONSE_TYPE_SHORT_TEXT:
                    // EXAMPLE USAGE OF RESPONSE VIEW
                    inflatedQuestionView.addView(inflateShortTextResponseView());
                    break;
            }
        }

        // create view for option here and store it in the map
        @Override
        protected void onOptionCreated(Option option) {

            ResponseType responseType = option.getResponseType();
            
            // ...... Do something here (inflate View, add listeners etc)......

            formOptionViews.put(option.getID(), inflatedOptionView);

        }

        @Override
        public SectionType getSectionTypeFromString(String sectionType) {
            // use this to return an enumeration from the encoded string
            return SectionType.SECTION_TYPE_DEFAULT;
        }

        @Override
        public QuestionType getQuestionTypeFromString(String questionType) {
            // use this to return an enumeration from the encoded string
            return QuestionType.QUESTION_TYPE_DEFAULT;
        }

        @Override
        public ResponseType getResponseTypeFromString(String responseType) {
            // use this to return an enumeration from the encoded string
            return ResponseType.RESPONSE_TYPE_DEFAULT;
        }
        
        @Override public boolean validateFormResponses() {
            // Enter your validation scheme here
            return true;
        }
    }
}
