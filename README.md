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
>ROTATE
>L5EZftvrYaSudioZVRzTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6

<li>END</li>
For solving WIF with missing characters at the end. Time needed depends on length of missing part, 7-8 characters should be found quickly.
Configuration file example, with 8 characters missing and searching for particular address (optional). 
>END
>L5EZftvrYaSudiozVRzTqLcHLNDoVn7H5HSfM9BAN6tM
>1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem

<li><SEARCH></li>
This mode expects input with marked positions of unknown characters. For each position it is possible to specify a set of suspected characters.
If set is not provided, program will use the full Base58 set.
Configuration file example with 6 missing characters in the middle. For the first 4 positions set of probable characters is provided (for characters 3,4&5 it is the whole Base58 set).
Program is looking for the given address.
>SEARCH
>L5EZftvrYaSudiozVRzTqLcHLND____H_HSfM9BAN6tMJX8oTWz6
>1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem
>acoeus
>AaBbcCNnMmXxkKhHvV
>*
>123456789ABCDEFGHIJKLMN

In this mode it is possible to resume search from the last known position (reported by program during work). To do this, the last known status could be provided in source WIF line:
>L5EZftvrYaSudiozVRzTqLcHLND____H_HSfM9BAN6tMJX8oTWz6,L5EZftvrYaSudiozVRzTqLcHLNDckk2H5HSfM9BAN6tMJX8oTWz6

<li>ALIKE</li>
This mode is similar to SEARCH, but instead of defining sets per position user may define groups of letters which will be tested instead of original one.
For example defining gruup `2Zz` means that during work each instance of 2 or z or Z will be replaced by one of the other characters in the group.
User may define any number of groups, but groups should not contain this same characters (in this case the first defined group will be used).
Using this mode could be helpful for example when handwritten WIF is unreadable or user have doubts how author writes some characters.
Mode expects full WIF (all characters should be 'known').
Configuration file example with several characters misspelled:
>ALIKE
>LSEzftvrYaSudie2VRzTqLcHLNDoVn7H5HSfM9BAN6tMJx8oTWz6
>1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem
>2Zz
>5Ss
>eco
>jJ
>vV
>xX

<li>JUMP</li>
Experimental mode for lost characters in the first part of WIF (expects the complete end of WIF).
Instead of simple brute-force (checking each possible character from Base58 set) this mode tries to find length of jump which keeps the end of decoded WIF untouched.
This method does not always suits the given WIF, but could bring some time-saving optimizations.
In this example jump length has found value 64, which means that program verifies every 64th key, so search is 64 times faster in comparison to checking every letter. 
>JUMP
>L5EZftvrYaSud_____zTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6
>1EUXSxuUVy2PC5enGXR1a3yxbEjNWMHuem

In this mode it is possible to resume search from the last known position (reported by program during work). To do this, the last known status could be provided in source WIF line:
>L5EZftvrYaSud_____zTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6,L5EZftvrYaSudAsCfDzTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6
</ol>

Please check files in `/examples/` folder to see templates of configuration.

Email notification
------------------

Passing as a second parameter path to email configuration allows program to send email notifications for start and end of work (with result if found).
This configuration is optional.


If you found this program useful, please donate: 34dEiyShGJcnGAg2jWhcoDDRxpennSZxg8 

<img style="display: block;" src="data:image/png;base64,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">

TODO
----
<ol>
<li>GUI</li>
<li>adapt number of threads used to user's machine</li>
<li>saving result and (periodically) status to file</li>
<li>new modes?</li>
</ol>
 