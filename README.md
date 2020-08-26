
# WifSolver
Tool for solving misspelled or damaged Bitcoin Private Key in Wallet Import Format (WIF)

Usage:
`java -jar wifSolver.jar configurationFile [optionally email configuration]`

If your problem cannot be covered by any of modes below, please contact me, I will try to modify program accordingly or help you fix the damaged WIF.

How to use it
-------------

In my examples I will use WIF _L5EZftvrYaSudiozVRzTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6_
which produces address _1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem_


Program could be launched in 5 modes:
<ol>
<li>ROTATE</li>

Suitable for solving WIF where one character is wrong and it's position is unknown.
Expects to receive full-length WIF. Program checks every single character and verifies if WIF is correct.
Configuration file example (misspelled letter Z/z on 16 position):

    ROTATE
    L5EZftvrYaSudioZVRzTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6

<li>END</li>
For solving WIF with missing characters at the end. Time needed depends on length of missing part, 7-8 characters should be found quickly.
Configuration file example, with 8 characters missing and searching for particular address (optional). 

    END
    L5EZftvrYaSudiozVRzTqLcHLNDoVn7H5HSfM9BAN6tM
    1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem

<li>SEARCH</li>
This mode expects input with marked positions of unknown characters. For each position it is possible to specify a set of suspected characters.
If set is not provided, program will use the full Base58 set.
Configuration file example with 6 missing characters in the middle. For the first 4 positions set of probable characters is provided (for characters 3&5 it is the whole Base58 set).
Program is looking for the given address.

    SEARCH
    L5EZftvrYaSudiozVRzTqLcHLND____H_HSfM9BAN6tMJX8oTWz6
    1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem
    acoeus
    AaBbcCNnMmXxkKhHvV
    *
    123456789ABCDEFGHIJKLMN

In this mode it is possible to resume search from the last known position (reported by program during work). To do this, the last known status could be provided in source WIF line:

    L5EZftvrYaSudiozVRzTqLcHLND____H_HSfM9BAN6tMJX8oTWz6,L5EZftvrYaSudiozVRzTqLcHLNDckk2H5HSfM9BAN6tMJX8oTWz6

<li>ALIKE</li>
This mode is similar to SEARCH, but instead of defining sets per position user may define groups of letters which will be tested instead of original one.
For example defining gruup `2Zz` means that during work each instance of 2 or z or Z will be replaced by one of the other characters in the group.
User may define any number of groups, but groups should not contain this same characters (in this case the first defined group will be used).
Using this mode could be helpful for example when handwritten WIF is unreadable or user have doubts how author writes some characters.
Mode expects full WIF (all characters should be 'known').
Configuration file example with several characters misspelled:

    ALIKE
    LSEzftvrYaSudie2VRzTqLcHLNDoVn7H5HSfM9BAN6tMJx8oTWz6
    1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem
    2Zz
    5Ss
    eco
    jJ
    vV
    xX


<li>JUMP</li>
Experimental mode for lost characters in the first part of WIF (expects the complete end of WIF).
Instead of simple brute-force (checking each possible character from Base58 set) this mode tries to find length of jump which keeps the end of decoded WIF untouched.
This method does not always suits the given WIF, but could bring some time-saving optimizations.
In this example jump length has found value 64, which means that program verifies every 64th key, so search is 64 times faster in comparison to checking every letter. 

    JUMP
    L5EZftvrYaSud_____zTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6
    1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem

In this mode it is possible to resume search from the last known position (reported by program during work). To do this, the last known status could be provided in source WIF line:

    L5EZftvrYaSud_____zTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6,L5EZftvrYaSudAsCfDzTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6

</ol>

Please check files in `/examples/` folder to see templates of configuration.

Email notification
------------------

Passing as a second parameter path to email configuration allows program to send email notifications for start and end of work (with result if found).
This configuration is optional.

Contact
-------
Contact email: crypto@pawelgorny.com
If you found this program useful, consider making a donation, I will appreciate it! 
**BTC**: `34dEiyShGJcnGAg2jWhcoDDRxpennSZxg8`


TODO
----
<ol>
<li>GUI</li>
<li>adapt number of threads used to user's machine</li>
<li>saving (periodically) status to file</li>
<li>new modes?</li>
</ol>
 
