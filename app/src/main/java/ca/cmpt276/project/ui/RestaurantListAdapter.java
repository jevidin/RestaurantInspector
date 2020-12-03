package ca.cmpt276.project.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import ca.cmpt276.project.R;
import ca.cmpt276.project.model.Inspection;
import ca.cmpt276.project.model.InspectionListManager;
import ca.cmpt276.project.model.Restaurant;
import ca.cmpt276.project.model.RestaurantListManager;

public class RestaurantListAdapter extends ArrayAdapter<Restaurant> {
    private final List<Restaurant> restaurantList;
    private final LayoutInflater inflater;
    private final Context context;

    public RestaurantListAdapter(Context context, List<Restaurant> list){
        super(context, R.layout.restaurant_list_item, list);
        inflater = LayoutInflater.from(context);
        this.restaurantList = list;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = convertView;
        if(itemView == null){
            itemView = inflater.inflate(R.layout.restaurant_list_item, parent, false);
        }
        // find restaurant
        Restaurant currentRestaurant = restaurantList.get(position);
        InspectionListManager currentInspectionList = currentRestaurant.getInspections();

        ImageView hazardImageView = itemView.findViewById(R.id.item_hazard_icon);
        ImageView restaurantLogo = itemView.findViewById(R.id.item_restaurantIcon);
        ImageView favourite = itemView.findViewById(R.id.img_fav);
        TextView nameText = itemView.findViewById(R.id.item_txt_restaurant_name);
        TextView issuesText = itemView.findViewById(R.id.item_txt_issues_found);
        TextView inspectionText = itemView.findViewById(R.id.item_txt_latest_inspection);
        TextView hazardText = itemView.findViewById(R.id.item_txt_hazard);
        nameText.setText(currentRestaurant.getName());

        TypedValue theme = new TypedValue();
        int currentNightMode = getContext().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        context.getTheme().resolveAttribute(R.attr.colorOnPrimary, theme,true);

        int favouriteColor = theme.data;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            if (currentRestaurant.isFavourite()) {
                //favourite.setBackgroundResource(R.drawable.fav_color);
                favouriteColor = context.getColor(R.color.cyan);
            }
        } else if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            if (currentRestaurant.isFavourite()) {
                //favourite.setBackgroundResource(R.drawable.fav_color);
                favouriteColor = context.getColor(R.color.sapphire);
            }
        }
        itemView.setBackgroundColor(favouriteColor);

        if(currentInspectionList.getInspections().size() > 0) {
            Inspection latestInspection;
            latestInspection = Collections.max(currentInspectionList.getInspections());
           String issues_txt = String.valueOf(R.string.num_issues);
           int num_issues = latestInspection.getCritical() + latestInspection.getNonCritical();
           // String issuesMessage = num_issues + issues_txt;

            String issuesMessage = context.getString(num_issues, issues_txt);

            issuesText.setText(issuesMessage);

            switch (latestInspection.getLevel()) {
                case LOW:
                    hazardText.setText(R.string.hazard_low);
                    hazardText.setTextColor(Color.parseColor("#45DE08")); // Green
                    hazardImageView.setBackgroundResource(R.drawable.green_hazard);
                    break;
                case MODERATE:
                    hazardText.setText(R.string.hazard_moderate);
                    hazardText.setTextColor(Color.parseColor("#FA9009")); // Orange
                    hazardImageView.setBackgroundResource(R.drawable.orange_hazard);
                    break;
                case HIGH:
                    hazardText.setText(R.string.hazard_high);
                    hazardText.setTextColor(Color.parseColor("#FA2828")); // Red
                    hazardImageView.setBackgroundResource(R.drawable.red_hazard);
                    break;
                default:
                    assert false;
            }

            if (currentRestaurant.getName().contains("7-Eleven")) {
                restaurantLogo.setBackgroundResource(R.drawable.seveneleven);
            }
            else if (currentRestaurant.getName().contains("Sushi") || currentRestaurant.getName().contains("japanese")) {
                restaurantLogo.setBackgroundResource(R.drawable.sushi_generic);
            }
            else if (currentRestaurant.getName().contains("Blenz")) {
                restaurantLogo.setBackgroundResource(R.drawable.blenz);
            }
            else if (currentRestaurant.getName().contains("Booster Juice")) {
                restaurantLogo.setBackgroundResource(R.drawable.boosterjuice);
            }
            else if (currentRestaurant.getName().contains("Boston Pizza")) {
                restaurantLogo.setBackgroundResource(R.drawable.bostonpizza);
            }
            else if (currentRestaurant.getName().contains("Browns Socialhouse")) {
                restaurantLogo.setBackgroundResource(R.drawable.boosterjuice);
            }
            else if (currentRestaurant.getName().contains("KFC")) {
                restaurantLogo.setBackgroundResource(R.drawable.kfc_chicken);
            }
            else if (currentRestaurant.getName().contains("Little Caesars Pizza")) {
                restaurantLogo.setBackgroundResource(R.drawable.littleceasers);
            }
            else if (currentRestaurant.getName().contains("McDonald's")) {
                restaurantLogo.setBackgroundResource(R.drawable.mcdonalds);
            }
            else if (currentRestaurant.getName().contains("A&W") || currentRestaurant.getName().contains("A & W")) {
                restaurantLogo.setBackgroundResource(R.drawable.a_and_w);
            }
            else if (currentRestaurant.getName().contains("Pizza Pizza")) {
                restaurantLogo.setBackgroundResource(R.drawable.pizzapizza);
            }
            else if (currentRestaurant.getName().contains("Pizza Hut")) {
                restaurantLogo.setBackgroundResource(R.drawable.pizza_hut);
            }
            else if (currentRestaurant.getName().contains("Pizza")) {
                restaurantLogo.setBackgroundResource(R.drawable.generic_pizza);
            }
            else if (currentRestaurant.getName().contains("Catering")) {
                restaurantLogo.setBackgroundResource(R.drawable.catering);
            }
            else if (currentRestaurant.getName().contains("Coffee")) {
                restaurantLogo.setBackgroundResource(R.drawable.coffee);
            }
            else if (currentRestaurant.getName().contains("Pho") || currentRestaurant.getName().contains("Vietnamese")) {
                restaurantLogo.setBackgroundResource(R.drawable.pho);
            }
            else if (currentRestaurant.getName().contains("Pub") || currentRestaurant.getName().contains("Bar")) {
                restaurantLogo.setBackgroundResource(R.drawable.coffee);
            }
            else if (currentRestaurant.getName().contains("Market") || currentRestaurant.getName().contains("Grocery")) {
                restaurantLogo.setBackgroundResource(R.drawable.market);
            }
            else {
                restaurantLogo.setBackgroundResource(R.drawable.food2);
            }

            LocalDate currentDate = LocalDate.now();

            String inspectionDateText;
            if(Math.abs(currentDate.getYear() - latestInspection.getDate().getYear()) != 0){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
                inspectionDateText = formatter.format(latestInspection.getDate());
            }
            else if(Math.abs(currentDate.getMonthValue() - latestInspection.getDate().getMonthValue()) != 0){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
                inspectionDateText = formatter.format(latestInspection.getDate());
            }
            else{
                inspectionDateText = latestInspection.getDate().getDayOfMonth() + " days ago";
            }
            inspectionText.setText(inspectionDateText);
        }
        else{
            issuesText.setText(R.string.no_inspection_found);
            inspectionText.setText("");
        }
        return itemView;
    }
}
