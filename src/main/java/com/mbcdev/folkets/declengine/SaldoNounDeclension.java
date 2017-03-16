package com.mbcdev.folkets.declengine;

/**
 * Model for a Saldo noun declengine
 *
 * Created by barry on 12/03/2017.
 */
@SuppressWarnings("unused")
class SaldoNounDeclension {

    private String form;
    private String msd;

    /**
     * Gets the form that the declension takes for the given {@link #getType() type} of declension
     * <p>
     *     An example is "_et" for singlular definitive nominative. If you take the word glas, then
     *     you would substitute that for the underscore, and get "glaset" (the glass).
     * </p>
     *
     * @return the form of the declension, or null if unknown.
     */
    String getForm() {
        return form;
    }

    /**
     * Gets the type of the declension.
     * <p>
     *     This is the form that the API returns.
     *
     *     <table>
     *         <tr>"sg indef nom", "sg indef gen",</tr>
     *         <tr>"sg def nom", "sg def gen",</tr>
     *         <tr>"pl indef nom", "pl indef gen",</tr>
     *         <tr>"pl def nom", "pl def gen"</tr>
     *     </table>
     * </p>
     *
     * @return the type of the declension, as returned by the API, or null if unknown
     */
    String getType() {
        return msd;
    }
}
