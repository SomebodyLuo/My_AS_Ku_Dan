package io.orbi.ar.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.orbi.ar.R;

/**
 * Created by Ian Thew on 8/22/17.
 */

public class MarkerFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.marker_fragment, container, false);
    }
}
