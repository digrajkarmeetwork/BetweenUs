package com.mmog.screens;

public enum ScreenEnum {

	MAIN_MENU {
        public AbstractScreen getScreen(Object... params) {
            return new MainScreen();
        }
    },
	LOBBY_SCREEN {
        public AbstractScreen getScreen(Object... params) {
            return new LobbyScreen();
        }
    },
	ROLE_UI {
        public AbstractScreen getScreen(Object... params) {
            return new RoleUI();
        }
    },
	JOIN_SCREEN {
        public AbstractScreen getScreen(Object... params) {
            return new JoinScreen();
        }
    },
	CREATE_ROOM {
        public AbstractScreen getScreen(Object... params) {
            return new CreateRoomScreen();
        }
    },
	END_SCREEN {
        public AbstractScreen getScreen(Object... params) {
            return new EndGameScreen();
        }
    },
    GAME {
        public AbstractScreen getScreen(Object... params) {
            return new GameScreen();
        }
    },
    SETTINGS {
        public AbstractScreen getScreen(Object... params) {
            return new SettingsScreen();
        }
    };

    public abstract AbstractScreen getScreen(Object... params);
}
