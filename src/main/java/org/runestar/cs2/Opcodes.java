package org.runestar.cs2;

public final class Opcodes {

    public static final int
            SS_AND = -2,
            SS_OR = -1;

    private Opcodes() {}

    public static final int
            PUSH_CONSTANT_INT = 0,
            GET_VAR = 1,
            SET_VAR = 2,
            PUSH_CONSTANT_STRING = 3,
            BRANCH = 6,
            BRANCH_NOT = 7,
            BRANCH_EQUALS = 8,
            BRANCH_LESS_THAN = 9,
            BRANCH_GREATER_THAN = 10,
            RETURN = 21,
            GET_VARBIT = 25,
            SET_VARBIT = 27,
            BRANCH_LESS_THAN_OR_EQUALS = 31,
            BRANCH_GREATER_THAN_OR_EQUALS = 32,
            PUSH_INT_LOCAL = 33,
            POP_INT_LOCAL = 34,
            PUSH_STRING_LOCAL = 35,
            POP_STRING_LOCAL = 36,
            JOIN_STRING = 37,
            POP_INT_DISCARD = 38,
            POP_STRING_DISCARD = 39,
            INVOKE = 40,
            GET_VARC_INT = 42,
            SET_VARC_INT = 43,
            DEFINE_ARRAY = 44,
            GET_ARRAY_INT = 45,
            SET_ARRAY_INT = 46,
            GET_VARC_STRING_OLD = 47,
            SET_VARC_STRING_OLD = 48,
            GET_VARC_STRING = 49,
            SET_VARC_STRING = 50,
            SWITCH = 60,
            CC_CREATE = 100,
            CC_DELETE = 101,
            CC_DELETEALL = 102,
            CC_FIND = 200,
            IF_FIND = 201,
            CC_SETPOSITION = 1000,
            CC_SETSIZE = 1001,
            CC_SETHIDE = 1003,
            CC_SETNOCLICKTHROUGH = 1005,
            _1006 = 1006,
            CC_SETSCROLLPOS = 1100,
            CC_SETCOLOUR = 1101,
            CC_SETFILL = 1102,
            CC_SETTRANS = 1103,
            CC_SETLINEWID = 1104,
            CC_SETGRAPHIC = 1105,
            CC_SET2DANGLE = 1106,
            CC_SETTILING = 1107,
            CC_SETMODEL = 1108,
            CC_SETMODELANGLE = 1109,
            CC_SETMODELANIM = 1110,
            CC_SETMODELORTHOG = 1111,
            CC_SETTEXT = 1112,
            CC_SETTEXTFONT = 1113,
            CC_SETTEXTALIGN = 1114,
            CC_SETTEXTSHADOW = 1115,
            CC_SETOUTLINE = 1116,
            CC_SETGRAPHICSHADOW = 1117,
            CC_SETVFLIP = 1118,
            CC_SETHFLIP = 1119,
            CC_SETSCROLLSIZE = 1120,
            CC_RESUME_PAUSEBUTTON = 1121,
            _1122 = 1122,
            CC_SETFILLCOLOUR = 1123,
            _1124 = 1124,
            _1125 = 1125,
            CC_SETLINEDIRECTION = 1126,
            _1127 = 1127,
            CC_SETOBJECT = 1200,
            CC_SETNPCHEAD = 1201,
            CC_SETPLAYERHEAD_SELF = 1202,
            CC_SETOBJECT_NONUM = 1205,
            CC_SETOBJECT_ALWAYS_NUM = 1212,
            CC_SETOP = 1300,
            CC_SETDRAGGABLE = 1301,
            CC_SETDRAGGABLEBEHAVIOR = 1302,
            CC_SETDRAGDEADZONE = 1303,
            CC_SETDRAGDEADTIME = 1304,
            CC_SETOPBASE = 1305,
            CC_SETTARGETVERB = 1306,
            CC_CLEAROPS = 1307,
            CC_SETOPKEY = 1350,
            CC_SETOPTKEY = 1351,
            CC_SETOPKEYRATE = 1352,
            CC_SETOPTKEYRATE = 1353,
            CC_SETOPKEYIGNOREHELD = 1354,
            CC_SETOPTKEYIGNOREHELD = 1355,
            CC_SETONCLICK = 1400,
            CC_SETONHOLD = 1401,
            CC_SETONRELEASE = 1402,
            CC_SETONMOUSEOVER = 1403,
            CC_SETONMOUSELEAVE = 1404,
            CC_SETONDRAG = 1405,
            CC_SETONTARGETLEAVE = 1406,
            CC_SETONVARTRANSMIT = 1407,
            CC_SETONTIMER = 1408,
            CC_SETONOP = 1409,
            CC_SETONDRAGCOMPLETE = 1410,
            CC_SETONCLICKREPEAT = 1411,
            CC_SETONMOUSEREPEAT = 1412,
            CC_SETONINVTRANSMIT = 1414,
            CC_SETONSTATTRANSMIT = 1415,
            CC_SETONTARGETENTER = 1416,
            CC_SETONSCROLLWHEEL = 1417,
            CC_SETONCHATTRANSMIT = 1418,
            CC_SETONKEY = 1419,
            CC_SETONFRIENDTRANSMIT = 1420,
            CC_SETONCLANTRANSMIT = 1421,
            CC_SETONMISCTRANSMIT = 1422,
            CC_SETONDIALOGABORT = 1423,
            CC_SETONSUBCHANGE = 1424,
            CC_SETONSTOCKTRANSMIT = 1425,
            _1426 = 1426,
            CC_SETONRESIZE = 1427,
            CC_GETX = 1500,
            CC_GETY = 1501,
            CC_GETWIDTH = 1502,
            CC_GETHEIGHT = 1503,
            CC_GETHIDE = 1504,
            CC_GETLAYER = 1505,
            CC_GETSCROLLX = 1600,
            CC_GETSCROLLY = 1601,
            CC_GETTEXT = 1602,
            CC_GETSCROLLWIDTH = 1603,
            CC_GETSCROLLHEIGHT = 1604,
            CC_GETMODELZOOM = 1605,
            CC_GETMODELANGLE_X = 1606,
            CC_GETMODELANGLE_Z = 1607,
            CC_GETMODELANGLE_Y = 1608,
            CC_GETTRANS = 1609,
            _1610 = 1610,
            CC_GETCOLOUR = 1611,
            CC_GETFILLCOLOUR = 1612,
            _1613 = 1613,
            _1614 = 1614,
            CC_GETINVOBJECT = 1700,
            CC_GETINVCOUNT = 1701,
            CC_GETID = 1702,
            CC_GETTARGETMASK = 1800,
            CC_GETOP = 1801,
            CC_GETOPBASE = 1802,
            CC_CALLONRESIZE = 1927,
            IF_SETPOSITION = 2000,
            IF_SETSIZE = 2001,
            IF_SETHIDE = 2003,
            IF_SETNOCLICKTHROUGH = 2005,
            _2006 = 2006,
            IF_SETSCROLLPOS = 2100,
            IF_SETCOLOUR = 2101,
            IF_SETFILL = 2102,
            IF_SETTRANS = 2103,
            IF_SETLINEWID = 2104,
            IF_SETGRAPHIC = 2105,
            IF_SET2DANGLE = 2106,
            IF_SETTILING = 2107,
            IF_SETMODEL = 2108,
            IF_SETMODELANGLE = 2109,
            IF_SETMODELANIM = 2110,
            IF_SETMODELORTHOG = 2111,
            IF_SETTEXT = 2112,
            IF_SETTEXTFONT = 2113,
            IF_SETTEXTALIGN = 2114,
            IF_SETTEXTSHADOW = 2115,
            IF_SETOUTLINE = 2116,
            IF_SETGRAPHICSHADOW = 2117,
            IF_SETVFLIP = 2118,
            IF_SETHFLIP = 2119,
            IF_SETSCROLLSIZE = 2120,
            IF_RESUME_PAUSEBUTTON = 2121,
            _2122 = 2122,
            IF_SETFILLCOLOUR = 2123,
            _2124 = 2124,
            _2125 = 2125,
            IF_SETLINEDIRECTION = 2126,
            _2127 = 2127,
            IF_SETOBJECT = 2200,
            IF_SETNPCHEAD = 2201,
            IF_SETPLAYERHEAD_SELF = 2202,
            IF_SETOBJECT_NONUM = 2205,
            IF_SETOBJECT_ALWAYS_NUM = 2212,
            IF_SETOP = 2300,
            IF_SETDRAGGABLE = 2301,
            IF_SETDRAGGABLEBEHAVIOR = 2302,
            IF_SETDRAGDEADZONE = 2303,
            IF_SETDRAGDEADTIME = 2304,
            IF_SETOPBASE = 2305,
            IF_SETTARGETVERB = 2306,
            IF_CLEAROPS = 2307,
            IF_SETOPKEY = 2350,
            IF_SETOPTKEY = 2351,
            IF_SETOPKEYRATE = 2352,
            IF_SETOPTKEYRATE = 2353,
            IF_SETOPKEYIGNOREHELD = 2354,
            IF_SETOPTKEYIGNOREHELD = 2355,
            IF_SETONCLICK = 2400,
            IF_SETONHOLD = 2401,
            IF_SETONRELEASE = 2402,
            IF_SETONMOUSEOVER = 2403,
            IF_SETONMOUSELEAVE = 2404,
            IF_SETONDRAG = 2405,
            IF_SETONTARGETLEAVE = 2406,
            IF_SETONVARTRANSMIT = 2407,
            IF_SETONTIMER = 2408,
            IF_SETONOP = 2409,
            IF_SETONDRAGCOMPLETE = 2410,
            IF_SETONCLICKREPEAT = 2411,
            IF_SETONMOUSEREPEAT = 2412,
            IF_SETONINVTRANSMIT = 2414,
            IF_SETONSTATTRANSMIT = 2415,
            IF_SETONTARGETENTER = 2416,
            IF_SETONSCROLLWHEEL = 2417,
            IF_SETONCHATTRANSMIT = 2418,
            IF_SETONKEY = 2419,
            IF_SETONFRIENDTRANSMIT = 2420,
            IF_SETONCLANTRANSMIT = 2421,
            IF_SETONMISCTRANSMIT = 2422,
            IF_SETONDIALOGABORT = 2423,
            IF_SETONSUBCHANGE = 2424,
            IF_SETONSTOCKTRANSMIT = 2425,
            _2426 = 2426,
            IF_SETONRESIZE = 2427,
            IF_GETX = 2500,
            IF_GETY = 2501,
            IF_GETWIDTH = 2502,
            IF_GETHEIGHT = 2503,
            IF_GETHIDE = 2504,
            IF_GETLAYER = 2505,
            IF_GETSCROLLX = 2600,
            IF_GETSCROLLY = 2601,
            IF_GETTEXT = 2602,
            IF_GETSCROLLWIDTH = 2603,
            IF_GETSCROLLHEIGHT = 2604,
            IF_GETMODELZOOM = 2605,
            IF_GETMODELANGLE_X = 2606,
            IF_GETMODELANGLE_Z = 2607,
            IF_GETMODELANGLE_Y = 2608,
            IF_GETTRANS = 2609,
            _2610 = 2610,
            IF_GETCOLOUR = 2611,
            IF_GETFILLCOLOUR = 2612,
            _2613 = 2613,
            _2614 = 2614,
            IF_GETINVOBJECT = 2700,
            IF_GETINVCOUNT = 2701,
            IF_HASSUB = 2702,
            IF_GETTOP = 2706,
            IF_GETTARGETMASK = 2800,
            IF_GETOP = 2801,
            IF_GETOPBASE = 2802,
            IF_CALLONRESIZE = 2927,
            MES = 3100,
            ANIM = 3101,
            IF_CLOSE = 3103,
            RESUME_COUNTDIALOG = 3104,
            RESUME_NAMEDIALOG = 3105,
            RESUME_STRINGDIALOG = 3106,
            OPPLAYER = 3107,
            IF_DRAGPICKUP = 3108,
            CC_DRAGPICKUP = 3109,
            MOUSECAM = 3110,
            GETREMOVEROOFS = 3111,
            SETREMOVEROOFS = 3112,
            OPENURL = 3113,
            RESUME_OBJDIALOG = 3115,
            BUG_REPORT = 3116,
            SETSHIFTCLICKDROP = 3117,
            SETSHOWMOUSEOVERTEXT = 3118,
            RENDERSELF = 3119,
            _3120 = 3120,
            _3121 = 3121,
            _3122 = 3122,
            _3123 = 3123,
            _3124 = 3124,
            SETSHOWMOUSECROSS = 3125,
            SETSHOWLOADINGMESSAGES = 3126,
            SETTAPTODROP = 3127,
            GETTAPTODROP = 3128,
            _3129 = 3129,
            _3130 = 3130,
            _3131 = 3131,
            GETCANVASSIZE = 3132,
            _3133 = 3133,
            _3134 = 3134,
            _3135 = 3135,
            _3136 = 3136,
            _3137 = 3137,
            _3138 = 3138,
            _3139 = 3139,
            _3140 = 3140,
            SETHIDEUSERNAME = 3141,
            GETHIDEUSERNAME = 3142,
            SETREMEMBERUSERNAME = 3143,
            GETREMEMBERUSERNAME = 3144,
            _3145 = 3145,
            SOUND_SYNTH = 3200,
            SOUND_SONG = 3201,
            SOUND_JINGLE = 3202,
            CLIENTCLOCK = 3300,
            INV_GETOBJ = 3301,
            INV_GETNUM = 3302,
            INV_TOTAL = 3303,
            INV_SIZE = 3304,
            STAT = 3305,
            STAT_BASE = 3306,
            STAT_XP = 3307,
            COORD = 3308,
            COORDX = 3309,
            COORDY = 3310,
            COORDZ = 3311,
            MAP_MEMBERS = 3312,
            INVOTHER_GETOBJ = 3313,
            INVOTHER_GETNUM = 3314,
            INVOTHER_TOTAL = 3315,
            STAFFMODLEVEL = 3316,
            REBOOTTIMER = 3317,
            MAP_WORLD = 3318,
            RUNENERGY_VISIBLE = 3321,
            RUNWEIGHT_VISIBLE = 3322,
            PLAYERMOD = 3323,
            WORLDFLAGS = 3324,
            MOVECOORD = 3325,
            ENUM_STRING = 3400,
            ENUM = 3408,
            ENUM_GETOUTPUTCOUNT = 3411,
            FRIEND_COUNT = 3600,
            FRIEND_GETNAME = 3601,
            FRIEND_GETWORLD = 3602,
            FRIEND_GETRANK = 3603,
            FRIEND_SETRANK = 3604,
            FRIEND_ADD = 3605,
            FRIEND_DEL = 3606,
            IGNORE_ADD = 3607,
            IGNORE_DEL = 3608,
            FRIEND_TEST = 3609,
            CLAN_GETCHATDISPLAYNAME = 3611,
            CLAN_GETCHATCOUNT = 3612,
            CLAN_GETCHATUSERNAME = 3613,
            CLAN_GETCHATUSERWORLD = 3614,
            CLAN_GETCHATUSERRANK = 3615,
            CLAN_GETCHATMINKICK = 3616,
            CLAN_KICKUSER = 3617,
            CLAN_GETCHATRANK = 3618,
            CLAN_JOINCHAT = 3619,
            CLAN_LEAVECHAT = 3620,
            IGNORE_COUNT = 3621,
            IGNORE_GETNAME = 3622,
            IGNORE_TEST = 3623,
            CLAN_ISSELF = 3624,
            CLAN_GETCHATOWNERNAME = 3625,
            CLAN_ISFRIEND = 3626,
            CLAN_ISIGNORE = 3627,
            _3628 = 3628,
            _3629 = 3629,
            _3630 = 3630,
            _3631 = 3631,
            _3632 = 3632,
            _3633 = 3633,
            _3634 = 3634,
            _3635 = 3635,
            _3636 = 3636,
            _3637 = 3637,
            _3638 = 3638,
            _3639 = 3639,
            _3640 = 3640,
            _3641 = 3641,
            _3642 = 3642,
            _3643 = 3643,
            _3644 = 3644,
            _3645 = 3645,
            _3646 = 3646,
            _3647 = 3647,
            _3648 = 3648,
            _3649 = 3649,
            _3650 = 3650,
            _3651 = 3651,
            _3652 = 3652,
            _3653 = 3653,
            _3654 = 3654,
            _3655 = 3655,
            _3656 = 3656,
            _3657 = 3657,
            STOCKMARKET_GETOFFERTYPE = 3903,
            STOCKMARKET_GETOFFERITEM = 3904,
            STOCKMARKET_GETOFFERPRICE = 3905,
            STOCKMARKET_GETOFFERCOUNT = 3906,
            STOCKMARKET_GETOFFERCOMPLETEDCOUNT = 3907,
            STOCKMARKET_GETOFFERCOMPLETEDGOLD = 3908,
            STOCKMARKET_ISOFFEREMPTY = 3910,
            STOCKMARKET_ISOFFERSTABLE = 3911,
            STOCKMARKET_ISOFFERFINISHED = 3912,
            STOCKMARKET_ISOFFERADDING = 3913,
            TRADINGPOST_SORTBY_NAME = 3914,
            TRADINGPOST_SORTBY_PRICE = 3915,
            TRADINGPOST_SORTFILTERBY_WORLD = 3916,
            TRADINGPOST_SORTBY_AGE = 3917,
            TRADINGPOST_SORTBY_COUNT = 3918,
            TRADINGPOST_GETTOTALOFFERS = 3919,
            TRADINGPOST_GETOFFERWORLD = 3920,
            TRADINGPOST_GETOFFERNAME = 3921,
            TRADINGPOST_GETOFFERPREVIOUSNAME = 3922,
            TRADINGPOST_GETOFFERAGE = 3923,
            TRADINGPOST_GETOFFERCOUNT = 3924,
            TRADINGPOST_GETOFFERPRICE = 3925,
            TRADINGPOST_GETOFFERITEM = 3926,
            ADD = 4000,
            SUB = 4001,
            MULTIPLY = 4002,
            DIV = 4003,
            RANDOM = 4004,
            RANDOMINC = 4005,
            INTERPOLATE = 4006,
            ADDPERCENT = 4007,
            SETBIT = 4008,
            CLEARBIT = 4009,
            TESTBIT = 4010,
            MOD = 4011,
            POW = 4012,
            INVPOW = 4013,
            AND = 4014,
            OR = 4015,
            SCALE = 4018,
            APPEND_NUM = 4100,
            APPEND = 4101,
            APPEND_SIGNNUM = 4102,
            LOWERCASE = 4103,
            FROMDATE = 4104,
            TEXT_GENDER = 4105,
            TOSTRING = 4106,
            COMPARE = 4107,
            PARAHEIGHT = 4108,
            PARAWIDTH = 4109,
            TEXT_SWITCH = 4110,
            ESCAPE = 4111,
            APPEND_CHAR = 4112,
            CHAR_ISPRINTABLE = 4113,
            CHAR_ISALPHANUMERIC = 4114,
            CHAR_ISALPHA = 4115,
            CHAR_ISNUMERIC = 4116,
            STRING_LENGTH = 4117,
            SUBSTRING = 4118,
            REMOVETAGS = 4119,
            STRING_INDEXOF_CHAR = 4120,
            STRING_INDEXOF_STRING = 4121,
            OC_NAME = 4200,
            OC_OP = 4201,
            OC_IOP = 4202,
            OC_COST = 4203,
            OC_STACKABLE = 4204,
            OC_CERT = 4205,
            OC_UNCERT = 4206,
            OC_MEMBERS = 4207,
            OC_PLACEHOLDER = 4208,
            OC_UNPLACEHOLDER = 4209,
            OC_FIND = 4210,
            OC_FINDNEXT = 4211,
            OC_FINDRESET = 4212,
            CHAT_GETFILTER_PUBLIC = 5000,
            CHAT_SETFILTER = 5001,
            CHAT_SENDABUSEREPORT = 5002,
            CHAT_GETHISTORY_BYTYPEANDLINE = 5003,
            CHAT_GETHISTORY_BYUID = 5004,
            CHAT_GETFILTER_PRIVATE = 5005,
            CHAT_SENDPUBLIC = 5008,
            CHAT_SENDPRIVATE = 5009,
            CHAT_PLAYERNAME = 5015,
            CHAT_GETFILTER_TRADE = 5016,
            CHAT_GETHISTORYLENGTH = 5017,
            CHAT_GETNEXTUID = 5018,
            CHAT_GETPREVUID = 5019,
            DOCHEAT = 5020,
            CHAT_SETMESSAGEFILTER = 5021,
            CHAT_GETMESSAGEFILTER = 5022,
            GETWINDOWMODE = 5306,
            SETWINDOWMODE = 5307,
            GETDEFAULTWINDOWMODE = 5308,
            SETDEFAULTWINDOWMODE = 5309,
            CAM_FORCEANGLE = 5504,
            CAM_GETANGLE_XA = 5505,
            CAM_GETANGLE_YA = 5506,
            CAM_SETFOLLOWHEIGHT = 5530,
            CAM_GETFOLLOWHEIGHT = 5531,
            LOGOUT = 5630,
            VIEWPORT_SETFOV = 6200,
            VIEWPORT_SETZOOM = 6201,
            VIEWPORT_CLAMPFOV = 6202,
            VIEWPORT_GETEFFECTIVESIZE = 6203,
            VIEWPORT_GETZOOM = 6204,
            VIEWPORT_GETFOV = 6205,
            WORLDLIST_FETCH = 6500,
            WORLDLIST_START = 6501,
            WORLDLIST_NEXT = 6502,
            WORLDLIST_SPECIFIC = 6506,
            WORLDLIST_SORT = 6507,
            _6511 = 6511,
            SETFOLLOWEROPSLOWPRIORITY = 6512,
            NC_PARAM = 6513,
            LC_PARAM = 6514,
            OC_PARAM = 6515,
            STRUCT_PARAM = 6516,
            ON_MOBILE = 6518,
            CLIENTTYPE = 6519,
            _6520 = 6520,
            _6521 = 6521,
            _6522 = 6522,
            _6523 = 6523,
            BATTERYLEVEL = 6524,
            BATTERYCHARGING = 6525,
            WIFIAVAILABLE = 6526,
            _6600 = 6600,
            WORLDMAP_GETMAPNAME = 6601,
            WORLDMAP_SETMAP = 6602,
            WORLDMAP_GETZOOM = 6603,
            WORLDMAP_SETZOOM = 6604,
            WORLDMAP_ISLOADED = 6605,
            WORLDMAP_JUMPTODISPLAYCOORD = 6606,
            WORLDMAP_JUMPTODISPLAYCOORD_INSTANT = 6607,
            WORLDMAP_JUMPTOSOURCECOORD = 6608,
            WORLDMAP_JUMPTOSOURCECOORD_INSTANT = 6609,
            WORLDMAP_GETDISPLAYPOSITION = 6610,
            WORLDMAP_GETCONFIGORIGIN = 6611,
            WORLDMAP_GETCONFIGSIZE = 6612,
            WORLDMAP_GETCONFIGBOUNDS = 6613,
            WORLDMAP_GETCONFIGZOOM = 6614,
            _6615 = 6615,
            WORLDMAP_GETCURRENTMAP = 6616,
            WORLDMAP_GETDISPLAYCOORD = 6617,
            _6618 = 6618,
            _6619 = 6619,
            _6620 = 6620,
            WORLDMAP_COORDINMAP = 6621,
            WORLDMAP_GETSIZE = 6622,
            _6623 = 6623,
            _6624 = 6624,
            _6625 = 6625,
            _6626 = 6626,
            _6627 = 6627,
            WORLDMAP_PERPETUALFLASH = 6628,
            WORLDMAP_FLASHELEMENT = 6629,
            WORLDMAP_FLASHELEMENTCATEGORY = 6630,
            WORLDMAP_STOPCURRENTFLASHES = 6631,
            WORLDMAP_DISABLEELEMENTS = 6632,
            WORLDMAP_DISABLEELEMENT = 6633,
            WORLDMAP_DISABLEELEMENTCATEGORY = 6634,
            WORLDMAP_GETDISABLEELEMENTS = 6635,
            WORLDMAP_GETDISABLEELEMENT = 6636,
            WORLDMAP_GETDISABLEELEMENTCATEGORY = 6637,
            _6638 = 6638,
            WORLDMAP_LISTELEMENT_START = 6639,
            WORLDMAP_LISTELEMENT_NEXT = 6640,
            MEC_TEXT = 6693,
            MEC_TEXTSIZE = 6694,
            MEC_CATEGORY = 6695,
            MEC_SPRITE = 6696,
            WORLDMAP_ELEMENT = 6697,
            _6698 = 6698,
            WORLDMAP_ELEMENTCOORD = 6699;
}