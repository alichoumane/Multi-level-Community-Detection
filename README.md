# Multi-level-Community-Detection
This software implements the algorithm "Multi-level Fuzzy Overlapping Community Detection Algorithm for Weighted Networks".

Article: Choumane, A. and Harkous, A. ‘Multi-level fuzzy overlapping community detection algorithm for weighted networks’, Int. J. Data Science. To appear.

#############
Compile
#############

To compile the source code again, execute ./compile.sh under Linux. Make sure you have JDK 8 installed.

#############
Run
#############

The minimum command to run the algorithm is:
java -jar MultiLevel.jar -f network.dat

where network.dat is an undirected, weighted network.

If your network is unweighted, use the following command:
java -jar MultiLevel.jar -f network.dat -uw

#############
Output
#############

The program will generate the communities of each level in two formats:
	- one community per line (files named onePerLine-communities-leveli.dat)
	- one node per line followed by the ID(s) of its communities (files named communities-leveli.dat)
In addition, the memberships are stored in memberships-leveli.dat for the level i.

Contact us for any question: ali.choumane@ul.edu.lb
