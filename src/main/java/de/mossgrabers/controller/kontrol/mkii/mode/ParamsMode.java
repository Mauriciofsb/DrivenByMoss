// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2020
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.kontrol.mkii.mode;

import de.mossgrabers.controller.kontrol.mkii.KontrolProtocolConfiguration;
import de.mossgrabers.controller.kontrol.mkii.TrackType;
import de.mossgrabers.controller.kontrol.mkii.controller.KontrolProtocolControlSurface;
import de.mossgrabers.framework.controller.valuechanger.IValueChanger;
import de.mossgrabers.framework.daw.IDeviceBank;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.IParameterBank;
import de.mossgrabers.framework.daw.IParameterPageBank;
import de.mossgrabers.framework.daw.data.IParameter;
import de.mossgrabers.framework.mode.device.ParameterMode;


/**
 * The parameters mode.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class ParamsMode extends ParameterMode<KontrolProtocolControlSurface, KontrolProtocolConfiguration>
{
    /**
     * Constructor.
     *
     * @param surface The control surface
     * @param model The model
     */
    public ParamsMode (final KontrolProtocolControlSurface surface, final IModel model)
    {
        super (surface, model, false);
    }


    /** {@inheritDoc} */
    @Override
    public int getKnobValue (final int index)
    {
        // Note: Since we need multiple value (more than 8), index is the MIDI CC of the knob

        final IValueChanger valueChanger = this.model.getValueChanger ();
        final IParameterBank bank = this.getBank ();

        if (index >= KontrolProtocolControlSurface.KONTROL_TRACK_VOLUME && index < KontrolProtocolControlSurface.KONTROL_TRACK_VOLUME + 8)
        {
            final IParameter parameter = bank.getItem (index - KontrolProtocolControlSurface.KONTROL_TRACK_VOLUME);
            return valueChanger.toMidiValue (parameter.getValue ());
        }

        if (index >= KontrolProtocolControlSurface.KONTROL_TRACK_PAN && index < KontrolProtocolControlSurface.KONTROL_TRACK_PAN + 8)
        {
            final IParameter parameter = bank.getItem (index - KontrolProtocolControlSurface.KONTROL_TRACK_PAN);
            return valueChanger.toMidiValue (parameter.getValue ());
        }

        final int scrollTracksState = (bank.canScrollBackwards () ? 1 : 0) + (bank.canScrollForwards () ? 2 : 0);

        final IDeviceBank deviceBank = this.cursorDevice.getDeviceBank ();
        final int scrollScenesState = (deviceBank.canScrollBackwards () ? 1 : 0) + (deviceBank.canScrollForwards () ? 2 : 0);

        final KontrolProtocolConfiguration configuration = this.surface.getConfiguration ();
        switch (index)
        {
            case KontrolProtocolControlSurface.KONTROL_NAVIGATE_BANKS:
                return (this.cursorDevice.canSelectPreviousFX () ? 1 : 0) + (this.cursorDevice.canSelectNextFX () ? 2 : 0);
            case KontrolProtocolControlSurface.KONTROL_NAVIGATE_TRACKS:
                return configuration.isFlipTrackClipNavigation () ? scrollScenesState : scrollTracksState;
            case KontrolProtocolControlSurface.KONTROL_NAVIGATE_CLIPS:
                return configuration.isFlipTrackClipNavigation () ? scrollTracksState : scrollScenesState;
            case KontrolProtocolControlSurface.KONTROL_NAVIGATE_SCENES:
            default:
                return 0;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void updateDisplay ()
    {
        final IValueChanger valueChanger = this.model.getValueChanger ();
        final IParameterBank bank = this.getBank ();

        final IParameterPageBank parameterPageBank = this.cursorDevice.getParameterPageBank ();
        final String selectedPage = parameterPageBank.getSelectedItem ();

        final int [] vuData = new int [16];
        for (int i = 0; i < 8; i++)
        {
            final IParameter parameter = bank.getItem (i);

            // Track Available
            this.surface.sendKontrolTrackSysEx (KontrolProtocolControlSurface.KONTROL_TRACK_AVAILABLE, TrackType.GENERIC, i);
            this.surface.sendKontrolTrackSysEx (KontrolProtocolControlSurface.KONTROL_TRACK_SELECTED, parameter.isSelected () ? 1 : 0, i);
            this.surface.sendKontrolTrackSysEx (KontrolProtocolControlSurface.KONTROL_TRACK_RECARM, 0, i);
            final String info = parameter.doesExist () ? parameter.getDisplayedValue (8) : " ";
            this.surface.sendKontrolTrackSysEx (KontrolProtocolControlSurface.KONTROL_TRACK_VOLUME_TEXT, 0, i, info);
            this.surface.sendKontrolTrackSysEx (KontrolProtocolControlSurface.KONTROL_TRACK_PAN_TEXT, 0, i, info);
            final String name = parameter.doesExist () ? this.cursorDevice.getName () + "\n" + selectedPage + "\n" + parameter.getName () : "None";
            this.surface.sendKontrolTrackSysEx (KontrolProtocolControlSurface.KONTROL_TRACK_NAME, 0, i, name);

            final int j = 2 * i;
            vuData[j] = valueChanger.toMidiValue (parameter.getModulatedValue ());
            vuData[j + 1] = valueChanger.toMidiValue (parameter.getModulatedValue ());
        }
        this.surface.sendKontrolTrackSysEx (KontrolProtocolControlSurface.KONTROL_TRACK_VU, 2, 0, vuData);
    }


    /** {@inheritDoc} */
    @Override
    public void selectPreviousItem ()
    {
        this.cursorDevice.getParameterBank ().scrollBackwards ();
    }


    /** {@inheritDoc} */
    @Override
    public void selectNextItem ()
    {
        this.cursorDevice.getParameterBank ().scrollForwards ();
    }


    /** {@inheritDoc} */
    @Override
    public void selectPreviousItemPage ()
    {
        this.cursorDevice.selectPrevious ();
    }


    /** {@inheritDoc} */
    @Override
    public void selectNextItemPage ()
    {
        this.cursorDevice.selectNext ();
    }
}
