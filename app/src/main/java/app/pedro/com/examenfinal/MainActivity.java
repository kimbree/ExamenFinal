package app.pedro.com.examenfinal;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

//import facebook
//import google

public class MainActivity extends YouTubeBaseActivity implements
        View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        YouTubePlayer.OnInitializedListener {

    //para google plus
    private static final int RC_SIGN_IN = 0;
    private GoogleApiClient mGoogleApiClient;

    private boolean progreso;
    private ConnectionResult connectionResult;
    private boolean firmaUsuario;
    private SignInButton signInButton;
    private TextView usuario, email_usuario;
    private LinearLayout PerfilIniciado, PerfilFinalizado;

    //callbackmanager para facebook
    CallbackManager callbackManager;

    //para youtube
    private static final int RECOVERY_DIALOG_REQUEST = 1;
    private YouTubePlayerView yPV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Confirguramos el boton de Facebook
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        AppEventsLogger.activateApp(this);

        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Toast.makeText(getApplication(),"Conectado Al Facebook",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(getApplication(),"Se a cancelado",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(getApplication(),"Ha habido un Error", Toast.LENGTH_LONG).show();
                    }
                });

        setContentView(R.layout.activity_main);

        //googleplus
        signInButton = (SignInButton) findViewById(R.id.googleplus);
        signInButton.setOnClickListener(this);

        usuario = (TextView) findViewById(R.id.usuario);
        email_usuario = (TextView) findViewById(R.id.email);

        PerfilFinalizado = (LinearLayout) findViewById(R.id.perfil_finalizado);
        PerfilIniciado = (LinearLayout) findViewById(R.id.perfil_iniciado);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Plus.API, Plus.PlusOptions.builder()
                        .build()).addScope(Plus.SCOPE_PLUS_LOGIN).build();

        //Youtube
        yPV = (YouTubePlayerView) findViewById(R.id.youtube);
        yPV.initialize("AIzaSyC_ncV4uAHBCBAGzh3yngNK3Bk3mT4OktI", this);

        // Este es el Banner
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        adView.loadAd(adRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //para facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);

        //para google plus
        switch (requestCode){
            case RC_SIGN_IN:
                if(resultCode == RESULT_OK){
                    firmaUsuario = false;
                }
                progreso = false;
                if(!mGoogleApiClient.isConnected()){
                    mGoogleApiClient.connect();
                }
                break;
        }

        //para youtube
        if(requestCode == RECOVERY_DIALOG_REQUEST){
            getYouTubePlayerProvider().initialize("AIzaSyC_ncV4uAHBCBAGzh3yngNK3Bk3mT4OktI", this);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Desde aqui es toda la conexion con google plus
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop(){
        super.onStop();
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
    }

    private void resolvesSignInError(){
        if(connectionResult.hasResolution()){
            try {
                progreso = true;
                connectionResult.startResolutionForResult(this, RC_SIGN_IN);
            }
            catch (IntentSender.SendIntentException e){
                progreso = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

        if(!result.hasResolution()){
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        if(!progreso){
            connectionResult = result;

            if (firmaUsuario){
                resolvesSignInError();
            }
        }

    }

    @Override
    public void onConnected(Bundle bundle) {

        firmaUsuario = false;
        Toast.makeText(this, "¡Conexión Exitosa!", Toast.LENGTH_LONG).show();
        informacionPerfil();

    }

    private void actualizarPerfil(boolean iniciado){
        if(iniciado){
            PerfilFinalizado.setVisibility(View.GONE);
            PerfilIniciado.setVisibility(View.VISIBLE);
        }
        else{
            PerfilFinalizado.setVisibility(View.VISIBLE);
            PerfilIniciado.setVisibility(View.GONE);
        }
    }

    private void informacionPerfil() {
        try{
            if(Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null){
                Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                String nombreUsuario = person.getDisplayName();
                String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

                usuario.setText(nombreUsuario);
                email_usuario.setText(email);

                actualizarPerfil(true);

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();
        actualizarPerfil(false);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.googleplus:
                googlePlusLogin();
                break;
        }

    }

    public void signIn(View v){
        googlePlusLogin();
    }

    public void logout(View v){
        googlePlusLogout();
    }

    private void googlePlusLogin(){
        if(!mGoogleApiClient.isConnected()){
            firmaUsuario = true;
            resolvesSignInError();
        }
    }

    private void googlePlusLogout(){
        if(mGoogleApiClient.isConnected()){
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            actualizarPerfil(false);
        }
    }


    //Desde aqui es configuracion de youtube
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if(!b){
            youTubePlayer.loadVideo("pywvLuqv-pY"); //este es el video q se va a reproduccir

            youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.CHROMELESS);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

        if(youTubeInitializationResult.isUserRecoverableError()){
            youTubeInitializationResult.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
        }
        else{
            String errorMessage = String.format(
                    getString(R.string.Error_Reproduccion), youTubeInitializationResult.toString());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private YouTubePlayer.Provider getYouTubePlayerProvider(){
        return (YouTubePlayerView) findViewById(R.id.youtube);
    }

}
