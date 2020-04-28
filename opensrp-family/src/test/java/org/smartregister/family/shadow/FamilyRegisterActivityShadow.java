package org.smartregister.family.shadow;

import android.support.v4.app.Fragment;

import org.smartregister.family.activity.BaseFamilyRegisterActivity;
import org.smartregister.family.fragment.BaseFamilyRegisterFragment;
import org.smartregister.family.model.BaseFamilyRegisterModel;
import org.smartregister.family.presenter.BaseFamilyRegisterPresenter;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.HashMap;

/**
 * Created by samuelgithengi on 4/28/20.
 */
public class FamilyRegisterActivityShadow extends BaseFamilyRegisterActivity {
    @Override
    protected void initializePresenter() {
        presenter = new BaseFamilyRegisterPresenter(this, new BaseFamilyRegisterModel());
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new BaseFamilyRegisterFragment() {
            @Override
            protected void initializePresenter() {
                //mocked method do nothing
            }

            @Override
            public void setAdvancedSearchFormData(HashMap<String, String> advancedSearchFormData) {
                //mocked method do nothing
            }

            @Override
            protected String getMainCondition() {
                return null;
            }

            @Override
            protected String getDefaultSortQuery() {
                return null;
            }
        };
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[0];
    }
}
