package com.randomEventAnalytics.helpers.examHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;

@Getter
public enum ExamItemModel {
	TINDERBOX(41154, ExamItemType.FIREMAKING),
	LOGS(41232, ExamItemType.FIREMAKING),
	LANTERN(41229, ExamItemType.FIREMAKING),
	AXE(41184, ExamItemType.FIREMAKING),
	CANDLESTICK(27102, ExamItemType.FIREMAKING),
	SHRIMP(41147, ExamItemType.FISHING),
	SARDINE(41173, ExamItemType.FISHING), // Sardine?
	HARPOON(41158, ExamItemType.FISHING), // Harpoon
	PIKE_1(41163, ExamItemType.FISHING), // Pike?
	TROUT(41217, ExamItemType.FISHING), // Trout
	BASS(41193, ExamItemType.FISHING), // Bass?
	TUNA(41209, ExamItemType.FISHING), // Tuna
	PIKE_2(41206, ExamItemType.FISHING), // Pike?
	SHARK(41166, ExamItemType.FISHING), // Shark
	SALMON(41204, ExamItemType.FISHING), // Salmon?
	CAKE(41202, ExamItemType.COOKING), // Cake
	CHEFS_HAT(41203, ExamItemType.COOKING), // Chefs Hat
	APRON(41190, ExamItemType.COOKING), // Apron
	BREAD(41172, ExamItemType.COOKING), // Bread
	PIE(41205, ExamItemType.COOKING), // Pie
	BOW(41171, ExamItemType.RANGED), // Bow
	GNOME_COCKTAIL_1(27097, ExamItemType.DRINK), // Gnome cocktail of some sort
	GNOME_COCKTAIL_2(28421, ExamItemType.DRINK), // Gnome cocktail of some sort
	TEA(41162, ExamItemType.DRINK), // Tea
	WHISKEY(41219, ExamItemType.DRINK), // Whiskey
	BEER(41152, ExamItemType.DRINK), // Beer
	COPPER_ORE(41170, ExamItemType.MINING), // Copper ore?
	PLATEBODY(27094, ExamItemType.MINING, ExamItemType.MELEE), // Platebody
	MEDIUM_HELM(41189, ExamItemType.MINING, ExamItemType.MELEE), // Med helm
	FULL_HELM(41178, ExamItemType.MINING, ExamItemType.MELEE), // Full helm
	PLATE_LEGS(41179, ExamItemType.MINING, ExamItemType.MELEE), // Plate legs
	INGOT(41153, ExamItemType.MINING), // Ingot
	PICKAXE_1(41194, ExamItemType.MINING), // Pickaxe
	HAMMER(41183, ExamItemType.MINING), // Hammer
	PICKAXE_2(41149, ExamItemType.MINING), // Pickaxe
	SWORD(41150, ExamItemType.MELEE), // Sword
	BATTLE_AXE(41176, ExamItemType.MELEE), // Battle-axe
	SCIMITAR(41192, ExamItemType.MELEE), // Scimitar
	// These shields may also be MELEE and MINING
	KITE_SHIELD(41200, ExamItemType.SHIELD), // Kite shield
	SQUARE_SHIELD_1(41169, ExamItemType.SHIELD), // Square shield
	SQUARE_SHIELD_2(41188, ExamItemType.SHIELD), // Square shield with sword on it?
	ROUND_SHIELD(41221, ExamItemType.SHIELD), // Round shield
	BOW_2(41198, ExamItemType.RANGED), // Bow 2
	BANANA(41222, ExamItemType.FRUIT), // Banana
	STRAWBERRY(41230, ExamItemType.FRUIT), // Strawberry
	REDBERRY(41207, ExamItemType.FRUIT), // Redberry
	PINEAPPLE(41214, ExamItemType.FRUIT), // Pineapple
	WATERMELON(41224, ExamItemType.FRUIT), // Watermelon
	ARROWS(41177, ExamItemType.RANGED), // Arrows
	CROSSBOW(41146, ExamItemType.RANGED), // Crossbow
	PLANT_POT(41208, ExamItemType.FARMING), // Plant pot?
	SECATEURS(41197, ExamItemType.FARMING), // Secateurs
	RAKE(41212, ExamItemType.FARMING), // Rake
	WATERING_CAN(41213, ExamItemType.FARMING), // Watering can
	TROWEL(41210, ExamItemType.FARMING), // Trowel
	SPADE(41155, ExamItemType.FARMING), // Spade
	MASK(41195, ExamItemType.HAT), // Mask
	JESTER_HAT(41196, ExamItemType.HAT), // Jester hat
	PIRATE_HAT(41187, ExamItemType.HAT), // Pirate hat
	LEDERHOSEN_HAT(41164, ExamItemType.HAT), // Lederhosen hat
	TIARA(41148, ExamItemType.HAT), // Tiara
	FROG_MASK(27101, ExamItemType.HAT), // Frog mask
	AIR_RUNE(41168, ExamItemType.MAGIC), // Air rune
	WATER_RUNE(41231, ExamItemType.MAGIC), // Water rune
	FIRE_RUNE(41215, ExamItemType.MAGIC), // Fire rune
	EARTH_STAFF(41157, ExamItemType.MAGIC), // Earth rune
	MAGIC_STAFF(41174, ExamItemType.MAGIC), // Magic staff
	RING(27091, ExamItemType.JEWELRY), // Ring
	NECKLACE(41216, ExamItemType.JEWELRY), // Necklace
	GEMSTONE(41151, ExamItemType.JEWELRY), // Gemstone?
	HOLY_SYMBOL(41159, ExamItemType.JEWELRY), // Holy symbol
	SMALL_KNIFE(41199, ExamItemType.OTHER), // Small knife?
	CHEESE(41161, ExamItemType.OTHER), // Cheese
	BONES(2674, ExamItemType.OTHER), // Bones
	POT(41223, ExamItemType.OTHER), // Pot
	PURE_ESSENCE(41182, ExamItemType.OTHER), // Pure essence? TODO: Check this, may be a rune?
	DYE(41175, ExamItemType.OTHER), // Dye?
	KEY(29232, ExamItemType.OTHER), // Key
	GARLIC(41226, ExamItemType.OTHER), // Garlic/Onion
	RANGER_BOOTS(41220, ExamItemType.BOOTS), // Ranger boots?
	RANGER_BOOTS_2(41186, ExamItemType.BOOTS), // Ranger boots?
	INSULATED_BOOTS(27104, ExamItemType.BOOTS), // Insulated boots?
	FIGHTING_BOOTS(41160, ExamItemType.BOOTS), // Fighting boots
	JUG(41225, ExamItemType.OTHER), // Jug
	SHEARS(41227, ExamItemType.OTHER), // Shears
	LEGENDS_CAPE(41167, ExamItemType.OTHER), // Legends cape
	COCKTAIL_SHAKER(979, ExamItemType.OTHER), // (Cocktail) Shaker
	COCKTAIL_SHAKER_2(27096, ExamItemType.OTHER), // (Cocktail) Shaker
	BOOK_1(41181, ExamItemType.OTHER); // Book (Collection Log?)
	private final int modelId;
	private final List<ExamItemType> examItemTypes;

	ExamItemModel(int modelId, ExamItemType... examItemTypes) {
		this.modelId = modelId;
		this.examItemTypes = Arrays.asList(examItemTypes);
	}

	public boolean isType(ExamItemType type) {
		return this.examItemTypes.contains(type);
	}

	private static final Map<Integer, ExamItemModel> MODELS;

	static {
		ImmutableMap.Builder<Integer, ExamItemModel> builder = new ImmutableMap.Builder<>();
		for (ExamItemModel examModel : values()) {
			builder.put(examModel.modelId, examModel);
		}
		MODELS = builder.build();
	}

	public static ExamItemModel getExamItemModel(int modelId) {
		return MODELS.get(modelId);
	}
}
