package com.luminositygames.smoothietycoon;

import java.util.ArrayList;

import com.luminositygames.smoothietycoon.entities.Advertisements;
import com.luminositygames.smoothietycoon.entities.Container;
import com.luminositygames.smoothietycoon.entities.Customer;
import com.luminositygames.smoothietycoon.entities.Player;
import com.luminositygames.smoothietycoon.entities.Recipe;
import com.luminositygames.smoothietycoon.entities.Statistics;
import com.luminositygames.smoothietycoon.ui.Achievements;
import com.luminositygames.smoothietycoon.ui.Effect;
import com.luminositygames.smoothietycoon.ui.Notifications;
import com.luminositygames.smoothietycoon.ui.Section;
import com.luminositygames.smoothietycoon.ui.Tips;
import com.luminositygames.smoothietycoon.util.Countdown;
import com.luminositygames.smoothietycoon.util.Fonts;
import com.luminositygames.smoothietycoon.util.Sounds;

/**
 * This file is part of Smoothie Tycoon
 * 
 * Copyright (c) 2013 - 2014 Luminosity Games
 * 
 * @author Alan Morel
 * @since July 1, 2014
 * @version 1.0
 */

public class Game {

	private Player player;
	private Recipe recipe;
	private Container container;
	private Statistics stats;
	private Countdown night;
	private Countdown lastSpawn;
	private ArrayList<Customer> customers;
	private int day;
	private int temperature;
	private int maxCustomers;
	private int totalCustomers;

	public Game() {
		this.player = new Player();
		this.recipe = new Recipe();
		this.container = new Container();
		this.stats = new Statistics();
		this.day = 0;
		Notifications.load();
		Achievements.load();
		startNewDay();
	}

	private void startNewDay() {
		this.stats.addEntry(day, player.getMoney());
		this.day ++;
		this.night = new Countdown(10 * 1000, false);
		this.temperature = SmoothieTycoon.random.nextInt(100);
		this.customers = new ArrayList<Customer>();
		this.maxCustomers = getMaxCustomers();
		this.lastSpawn = new Countdown(getSpawnDelay(), true);
		this.totalCustomers = 0;
		Advertisements.useAds();
		Achievements.progress(Achievements.DAY, 1);
		Sounds.play("morning", 0.20f);
	}

	public Player getPlayer() {
		return player;
	}

	public Recipe getRecipe() {
		return recipe;
	}

	public Container getContainer() {
		return container;
	}

	public Statistics getStats(){
		return stats;
	}

	public Countdown getNight() {
		return night;
	}

	public int getDay() {
		return day;
	}

	public int getTemperature() {
		return temperature;
	}

	public void render(Section section, float delta) {
		renderEffects();
		if (section.isStand()){
			renderStand();
		}
		Notifications.render();
	}

	private void renderStand() {
		for (Customer customer : customers){
			customer.render();
		}
		if (!night.hasStarted()){
			Fonts.center(getBuyPercentage() + "% will buy", 660, Fonts.BLACK_36);
		}
	}

	private void renderEffects() {
		if (container.getServings() <= 1){
			Effect.render(Effect.SMOOTHIE);
		}
		if (player.getFruits() <= 9){
			Effect.render(Effect.FRUIT);
		}
		if (player.getIce() <= 9){
			Effect.render(Effect.ICE);
		}
		if (player.getYogurt() <= 9){
			Effect.render(Effect.YOGURT);
		}
		if (player.getJuice() <= 9){
			Effect.render(Effect.JUICE);
		}
		if (player.getCups() <= 3){
			Effect.render(Effect.CUPS);
		}
	}

	public void update(float delta) {
		Effect.update(delta);
		Notifications.update();
		updateCustomers(delta);
		processPurchases();
		if (totalCustomers < maxCustomers) {
			addNewCustomer();
		} else if (!dayStillRunning()) {
			if (!getNight().hasStarted()) {
				getNight().start();
				Tips.displayTip();
			} else if(getNight().isCompleted()) {
				startNewDay();
			}
		}
	}

	private void updateCustomers(float delta){
		boolean canBuy = getContainer().getServings() - getWaiting() > 0 && player.getCups() > 0;
		for (Customer customer : customers) {
			customer.update(delta, canBuy);
		}
	}

	private void processPurchases(){
		for (int i = 0; i < getPurchases(); i++) {
			if (container.getServings() <= 0 || player.getCups() <= 0){
				return;
			}
			if (recipe.getPrice() >= 5.00){
				Achievements.progress(Achievements.HIGH_PRICE, 1);
			}
			if (container.getFruit() + container.getJuice() + container.getYogurt() >= 60){
				Achievements.progress(Achievements.HIGH_QUALITY, 1);
			}
			getPlayer().addMoney(getRecipe().getPrice());
			getContainer().serve();
			getPlayer().useCup();
			Sounds.play("purchase", 0.5f);
			Achievements.progress(Achievements.CUP_SOLD, 1);
		}
	}

	private int getMaxCustomers(){
		int base = 20;
		int dayBonus = day * 3;
		int adBonus = Advertisements.getTotalCustomers();
		int max = base + dayBonus + adBonus;
		return max;
	}

	public void setNewMaxCustomers() {
		maxCustomers = getMaxCustomers();
	}

	private void addNewCustomer() {
		if (lastSpawn.isCompleted()){
			totalCustomers++;
			customers.add(new Customer(getWillBuy()));
			lastSpawn = new Countdown(getSpawnDelay(), true);
		}
	}

	private int getSpawnDelay() {
		int base = 1000 - Advertisements.getTotalCustomers() * 10;
		int variable = 500;
		int spawnDelay = SmoothieTycoon.random.nextInt(variable) + base;
		return spawnDelay;
	}

	private boolean dayStillRunning() {
		for (Customer customer : customers){
			if (!customer.hasLeft()){
				return true;
			}
		}
		return false;
	}

	private int getPurchases() {
		int purchases = 0;
		for (Customer customer : customers){
			if (customer.isBuying()){
				purchases++;
			}
		}
		return purchases;
	}

	private int getWaiting() {
		int waiting = 0;
		for (Customer customer : customers){
			if (customer.isWaiting()){
				waiting++;
			}
		}
		return waiting;
	}

	private boolean getWillBuy(){
		return SmoothieTycoon.random.nextInt(100) < getBuyPercentage();
	}

	private int getPricePercentageChange(){
		double optimalPrice = 0.25 + day * 0.03;
		int delta = (int) Math.round((optimalPrice - recipe.getPrice()) * 25);
		int percentage = delta * 5 + 75;
		return SmoothieTycoon.fixPercentage(percentage);
	}

	private int getTemperatureIceChange(){
		int optimalIce = temperature / 5;
		int delta = Math.abs(optimalIce - container.getIce());
		int percentage = 100 - delta * 10;
		return SmoothieTycoon.fixPercentage(percentage);
	}

	private int getQualityChange(){
		int standardQuality = 30;
		int currentQuality = container.getFruit() + container.getJuice() + container.getYogurt();
		int delta = currentQuality - standardQuality;
		int percentage = delta * 4;
		System.out.println("Quality percentage: " + percentage);
		return SmoothieTycoon.fixPercentage(percentage);
	}

	private int getBuyPercentage(){
		int percentage = 0;
		int pricePerc = getPricePercentageChange() * 60 / 100;
		int tempPerc = getTemperatureIceChange() * 20 / 100;
		int qualityPerc = getQualityChange() * 20 / 100;
		percentage += pricePerc;
		percentage += tempPerc;
		percentage += qualityPerc;
		return percentage;
	}
}
