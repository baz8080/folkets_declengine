import com.mbcdev.folkets.declension.SaldoNounDeclension;
import com.mbcdev.folkets.declension.SaldoService;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.List;


/**
 * Test class
 *
 * Created by barry on 12/03/2017.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        // Empty for now
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://spraakbanken.gu.se")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SaldoService saldoService = retrofit.create(SaldoService.class);

        Call<List<SaldoNounDeclension>> call = saldoService.getNounDeclensions("nn_0v_bikarbonat");

        Response<List<SaldoNounDeclension>> response = call.execute();

        if (response.isSuccessful()) {
            List<SaldoNounDeclension> nounDeclensions = response.body();

            for (SaldoNounDeclension nounDeclension : nounDeclensions) {
                System.out.println(nounDeclension.getForm());
                System.out.println(nounDeclension.getMsd());
            }
        }
    }
}
