package org.smartregister.family.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.FormEntityConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Photo;
import org.smartregister.domain.ProfileImage;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.ImageRepository;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.FormUtils;
import org.smartregister.util.ImageUtils;
import org.smartregister.view.LocationPickerView;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Created by keyman on 13/11/2018.
 */
public class JsonFormUtils extends org.smartregister.util.JsonFormUtils {
    private static final String TAG = JsonFormUtils.class.getCanonicalName();

    public static final String METADATA = "metadata";
    public static final String ENCOUNTER_TYPE = "encounter_type";
    public static final int REQUEST_CODE_GET_JSON = 2244;

    public static final String CURRENT_OPENSRP_ID = "current_opensrp_id";
    public static final String READ_ONLY = "read_only";

    public static JSONObject getFormAsJson(JSONObject form,
                                           String formName, String id,
                                           String currentLocationId) throws Exception {
        if (form == null) {
            return null;
        }

        String entityId = id;
        form.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);

        if (Constants.JSON_FORM.FAMILY_REGISTER.equals(formName)) {
            if (StringUtils.isNotBlank(entityId)) {
                entityId = entityId.replace("-", "");
            }

            // Inject opensrp id into the form
            JSONArray field = fields(form);
            JSONObject uniqueId = getFieldJSONObject(field, DBConstants.KEY.UNIQUE_ID);
            if (uniqueId != null) {
                uniqueId.remove(JsonFormUtils.VALUE);
                uniqueId.put(JsonFormUtils.VALUE, entityId);
            }

        } else {
            Log.w(TAG, "Unsupported form requested for launch " + formName);
        }
        Log.d(TAG, "form is " + form.toString());
        return form;
    }


    public static Pair<Client, Event> processFamilyRegistrationForm(AllSharedPreferences allSharedPreferences, String jsonString) {

        try {
            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);

            if (!registrationFormParams.getLeft()) {
                return null;
            }

            JSONObject jsonForm = registrationFormParams.getMiddle();
            JSONArray fields = registrationFormParams.getRight();

            String entityId = getString(jsonForm, ENTITY_ID);
            if (StringUtils.isBlank(entityId)) {
                entityId = generateRandomUUIDString();
            }

            String encounterType = getString(jsonForm, ENCOUNTER_TYPE);
            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            // String lastLocationName = null;
            // String lastLocationId = null;
            // TODO Replace values for location questions with their corresponding location IDs


            JSONObject lastInteractedWith = new JSONObject();
            lastInteractedWith.put(Constants.KEY.KEY, DBConstants.KEY.LAST_INTERACTED_WITH);
            lastInteractedWith.put(Constants.KEY.VALUE, Calendar.getInstance().getTimeInMillis());
            fields.put(lastInteractedWith);

            JSONObject dobUnknownObject = getFieldJSONObject(fields, DBConstants.KEY.DOB_UNKNOWN);
            JSONArray options = getJSONArray(dobUnknownObject, Constants.JSON_FORM_KEY.OPTIONS);
            JSONObject option = getJSONObject(options, 0);
            String dobUnKnownString = option != null ? option.getString(VALUE) : null;
            if (StringUtils.isNotBlank(dobUnKnownString) && Boolean.valueOf(dobUnKnownString)) {

                String ageString = getFieldValue(fields, DBConstants.KEY.AGE);
                if (StringUtils.isNotBlank(ageString) && NumberUtils.isNumber(ageString)) {
                    int age = Integer.valueOf(ageString);
                    JSONObject dobJSONObject = getFieldJSONObject(fields, DBConstants.KEY.DOB);
                    dobJSONObject.put(VALUE, Utils.getDob(age));

                    //Mark the birth date as an approximation
                    JSONObject isBirthdateApproximate = new JSONObject();
                    isBirthdateApproximate.put(Constants.KEY.KEY, FormEntityConstants.Person.birthdate_estimated);
                    isBirthdateApproximate.put(Constants.KEY.VALUE, Constants.BOOLEAN_INT.TRUE);
                    isBirthdateApproximate.put(Constants.OPENMRS.ENTITY, Constants.ENTITY.PERSON);//Required for value to be processed
                    isBirthdateApproximate.put(Constants.OPENMRS.ENTITY_ID, FormEntityConstants.Person.birthdate_estimated);
                    fields.put(isBirthdateApproximate);

                }
            }

            FormTag formTag = new FormTag();
            formTag.providerId = allSharedPreferences.fetchRegisteredANM();
            formTag.appVersion = FamilyLibrary.getInstance().getApplicationVersion();
            formTag.databaseVersion = FamilyLibrary.getInstance().getDatabaseVersion();


            Client baseClient = org.smartregister.util.JsonFormUtils.createBaseClient(fields, formTag, entityId);
            Event baseEvent = org.smartregister.util.JsonFormUtils.createEvent(fields, metadata, formTag, entityId, encounterType, DBConstants.FAMILY_TABLE_NAME);

            JsonFormUtils.tagSyncMetadata(allSharedPreferences, baseEvent);// tag docs

            return Pair.create(baseClient, baseEvent);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }


    public static void mergeAndSaveClient(ECSyncHelper ecUpdater, Client baseClient) throws Exception {
        JSONObject updatedClientJson = new JSONObject(org.smartregister.util.JsonFormUtils.gson.toJson(baseClient));

        JSONObject originalClientJsonObject = ecUpdater.getClient(baseClient.getBaseEntityId());

        JSONObject mergedJson = org.smartregister.util.JsonFormUtils.merge(originalClientJsonObject, updatedClientJson);

        //TODO Save edit log ?

        ecUpdater.addClient(baseClient.getBaseEntityId(), mergedJson);
    }

    public static void saveImage(String providerId, String entityId, String imageLocation) {
        if (StringUtils.isBlank(imageLocation)) {
            return;
        }

        File file = new File(imageLocation);

        if (!file.exists()) {
            return;
        }

        Bitmap compressedImageFile = FamilyLibrary.getInstance().getCompressor().compressToBitmap(file);
        saveStaticImageToDisk(compressedImageFile, providerId, entityId);

    }


    public static String getAutoPopulatedJsonEditFormString(Context context, CommonPersonObjectClient client) {
        try {
            JSONObject form = FormUtils.getInstance(context).getFormJson(Constants.JSON_FORM.FAMILY_REGISTER);
            LocationPickerView lpv = new LocationPickerView(context);
            lpv.init();
            // JsonFormUtils.addWomanRegisterHierarchyQuestions(form);
            Log.d(TAG, "Form is " + form.toString());
            if (form != null) {
                form.put(JsonFormUtils.ENTITY_ID, client.getCaseId());
                form.put(JsonFormUtils.ENCOUNTER_TYPE, Constants.EventType.UPDATE_FAMILY_REGISTRATION);

                JSONObject metadata = form.getJSONObject(JsonFormUtils.METADATA);
                String lastLocationId = LocationHelper.getInstance().getOpenMrsLocationId(lpv.getSelectedItem());

                metadata.put(JsonFormUtils.ENCOUNTER_LOCATION, lastLocationId);

                form.put(JsonFormUtils.CURRENT_OPENSRP_ID, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false));

                //inject opensrp id into the form
                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    processPopulatableFields(client, jsonObject);

                }

                return form.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return "";
    }

    protected static void processPopulatableFields(CommonPersonObjectClient client, JSONObject jsonObject) throws JSONException {


        if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase(DBConstants.KEY.DOB)) {

            String dobString = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
            if (StringUtils.isNotBlank(dobString)) {
                Date dob = Utils.dobStringToDate(dobString);
                if (dob != null) {
                    jsonObject.put(JsonFormUtils.VALUE, dd_MM_yyyy.format(dob));
                }
            }

        } else if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase(Constants.KEY.PHOTO)) {

            Photo photo = ImageUtils.profilePhotoByClientID(client.getCaseId(), Utils.getProfileImageResourceIDentifier());

            if (StringUtils.isNotBlank(photo.getFilePath())) {

                jsonObject.put(JsonFormUtils.VALUE, photo.getFilePath());

            }
        } else if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase(DBConstants.KEY.DOB_UNKNOWN)) {

            jsonObject.put(JsonFormUtils.READ_ONLY, false);
            JSONObject optionsObject = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS).getJSONObject(0);
            optionsObject.put(JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB_UNKNOWN, false));

        } else if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase(DBConstants.KEY.AGE)) {

            jsonObject.put(JsonFormUtils.READ_ONLY, false);
            String dobString = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
            if (StringUtils.isNotBlank(dobString)) {
                jsonObject.put(JsonFormUtils.VALUE, Utils.getAgeFromDate(dobString));
            }

        } else if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase(DBConstants.KEY.UNIQUE_ID)) {

            String uniqueId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false);
            jsonObject.put(JsonFormUtils.VALUE, uniqueId.replace("-", ""));

        } else {
            Log.e(TAG, "ERROR:: Unprocessed Form Object Key " + jsonObject.getString(JsonFormUtils.KEY));
        }
    }

    private static void saveStaticImageToDisk(Bitmap image, String providerId, String entityId) {
        if (image == null || StringUtils.isBlank(providerId) || StringUtils.isBlank(entityId)) {
            return;
        }
        OutputStream os = null;
        try {

            if (entityId != null && !entityId.isEmpty()) {
                final String absoluteFileName = DrishtiApplication.getAppDir() + File.separator + entityId + ".JPEG";

                File outputFile = new File(absoluteFileName);
                os = new FileOutputStream(outputFile);
                Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                if (compressFormat != null) {
                    image.compress(compressFormat, 100, os);
                } else {
                    throw new IllegalArgumentException("Failed to save static image, could not retrieve image compression format from name "
                            + absoluteFileName);
                }
                // insert into the db
                ProfileImage profileImage = new ProfileImage();
                profileImage.setImageid(UUID.randomUUID().toString());
                profileImage.setAnmId(providerId);
                profileImage.setEntityID(entityId);
                profileImage.setFilepath(absoluteFileName);
                profileImage.setFilecategory("profilepic");
                profileImage.setSyncStatus(ImageRepository.TYPE_Unsynced);
                ImageRepository imageRepo = FamilyLibrary.getInstance().context().imageRepository();
                imageRepo.add(profileImage);
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to save static image to disk");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close static images output stream after attempting to write image");
                }
            }
        }

    }

    protected static Triple<Boolean, JSONObject, JSONArray> validateParameters(String jsonString) {

        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = fields(jsonForm);

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = Triple.of(jsonForm != null && fields != null, jsonForm, fields);
        return registrationFormParams;
    }

    private static Event tagSyncMetadata(AllSharedPreferences allSharedPreferences, Event event) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        event.setProviderId(providerId);
        event.setLocationId(allSharedPreferences.fetchDefaultLocalityId(providerId));
        event.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        event.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));
        return event;
    }

}
