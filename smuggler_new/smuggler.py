#!/usr/bin/python3
import argparse
import re
import time
import sys
import os
import random
import string
import importlib
import hashlib
from copy import deepcopy
from time import sleep
from datetime import datetime
from lib.Payload import Payload, Chunked, EndChunk
from lib.EasySSL import EasySSL
from lib.colorama import Fore, Style
from urllib.parse import urlparse

class Desyncr():
    """
    Class to perform HTTP Desynchronization attacks.

    Attributes:
        _configfile (str): Path to the configuration file.
        _host (str): Target host for the attack.
        _port (int): Target port, default is 443 for HTTPS.
        _method (str): HTTP method to use, default is POST.
        _endpoint (str): Endpoint to target.
        _vhost (str): Virtual host if specified.
        _url (str): URL to target.
        _timeout (float): Timeout for connections.
        ssl_flag (bool): Flag to determine if SSL is used.
        _logh (file): Log handler for logging.
        _quiet (bool): Quiet mode flag.
        _exit_early (bool): Flag to exit early on success.
        _attempts (int): Number of attempts made.
        _cookies (list): List to store cookies.
    """

    def __init__(self, configfile, smhost, smport=443, url="", method="POST", endpoint="/", SSLFlag=False, logh=None, smargs=None):
        """
        Initialize the Desyncr class with configuration and connection details.

        Args:
            configfile (str): Path to the configuration file.
            smhost (str): Target host for the attack.
            smport (int, optional): Target port, default is 443 for HTTPS.
            url (str, optional): URL to target, default is an empty string.
            method (str, optional): HTTP method to use, default is POST.
            endpoint (str, optional): Endpoint to target, default is "/".
            SSLFlag (bool, optional): Flag to determine if SSL is used, default is False.
            logh (file, optional): Log handler for logging, default is None.
            smargs (object, optional): Object containing additional arguments, default is None.
        """
        self._configfile = configfile
        self._host = smhost
        self._port = smport
        self._method = method
        self._endpoint = endpoint
        self._vhost = smargs.vhost
        self._url = url
        self._timeout = float(smargs.timeout)
        self.ssl_flag = SSLFlag
        self._logh = logh
        self._quiet = smargs.quiet
        self._exit_early = smargs.exit_early
        self._attempts = 0
        self._cookies = []

    def _test(self, payload_obj):
        """
        Test the given payload against the target server.

        Args:
            payload_obj (object): Payload object to test.

        Returns:
            tuple: A tuple containing the result code, response, and payload object.
        """
        try:
            web = EasySSL(self.ssl_flag)
            web.connect(self._host, self._port, self._timeout)
            web.send(str(payload_obj).encode())
            start_time = datetime.now()
            res = web.recv_nb(self._timeout)
            end_time = datetime.now()
            web.close()
            if res is None:
                delta_time = end_time - start_time
                if delta_time.seconds < (self._timeout-1):
                    return (2, res, payload_obj)
                return (1, res, payload_obj)
            res_filtered = ""
            for single in res:
                if single > 0x7F:
                    res_filtered += '\x30'
                else:
                    res_filtered += chr(single)
            res = res_filtered
            return (0, res, payload_obj)
        except Exception as exception_data:
            return (-1, None, payload_obj)

    def _get_cookies(self):
        """
        Retrieve cookies from the target server.

        Returns:
            bool: True if cookies were retrieved successfully, False otherwise.
        """
        RN = "\r\n"
        try:
            cookies = []
            web = EasySSL(self.ssl_flag)
            web.connect(self._host, self._port, 2.0)
            p = Payload()
            p.host = self._host
            p.method = "GET"
            p.endpoint = self._endpoint
            p.header  = "__METHOD__ __ENDPOINT__?cb=__RANDOM__ HTTP/1.1" + RN
            p.header += "Host: __HOST__" + RN
            p.header += "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36" + RN
            p.header += "Content-type: application/x-www-form-urlencoded; charset=UTF-8" + RN
            p.header += "Content-Length: 0" + RN
            p.body = ""
            web.send(str(p).encode())
            sleep(0.5)
            res = web.recv_nb(2.0)
            web.close()
            if (res is not None):
                res = res.decode().split("\r\n")
                for elem in res:
                    if len(elem) > 11:
                        if elem[0:11].lower().replace(" ", "") == "set-cookie:":
                            cookie = elem.lower().replace("set-cookie:","")
                            cookie = cookie.split(";")[0] + ';'
                            cookies += [cookie]
                info = ((Fore.CYAN + str(len(cookies))+ Fore.MAGENTA), self._logh)
                print_info("Cookies    : %s (Appending to the attack)" % (info[0]))
                self._cookies += cookies
            return True
        except Exception as exception_data:
            error = ((Fore.CYAN + "Unable to connect to host"+ Fore.MAGENTA), self._logh)
            print_info("Error      : %s" % (error[0]))
            return False

    def run(self):
        """
        Main method to run the desynchronization attack.
        """
        RN = "\r\n"
        mutations = {}

        if not self._get_cookies():
            return

        if (self._configfile[1] != '/'):
            self._configfile = os.path.dirname(os.path.realpath(__file__)) + "/configs/" + self._configfile

        try:
            f = open(self._configfile)
        except:
            error = ((Fore.CYAN + "Cannot find config file"+ Fore.MAGENTA), self._logh)
            print_info("Error      : %s" % (error[0]))
            exit(1)

        script = f.read()
        f.close()

        exec(script)

        for mutation_name in mutations.keys():
            if self._create_exec_test(mutation_name, mutations[mutation_name]) and self._exit_early:
                break

        if self._quiet:
            sys.stdout.write("\r"+" "*100+"\r")

    def _check_tecl(self, payload, ptype=0):
        """
        Check for Transfer-Encoding: Chunked (TECL) vulnerability.

        Args:
            payload (object): Payload object to test.
            ptype (int, optional): Type of payload, default is 0.

        Returns:
            tuple: A tuple containing the result code, response, and payload object.
        """
        te_payload = deepcopy(payload)
        if (self._vhost == ""):
            te_payload.host = self._host
        else:
            te_payload.host = self._vhost
        te_payload.method = self._method
        te_payload.endpoint = self._endpoint

        if len(self._cookies) > 0:
            te_payload.header += "Cookie: " + ''.join(self._cookies) + "\r\n"

        if not ptype:
            te_payload.cl = 6
        else:
            te_payload.cl = 5

        te_payload.body = EndChunk+"X"
        return self._test(te_payload)

    def _check_clte(self, payload, ptype=0):
        """
        Check for Content-Length: (CLTE) vulnerability.

        Args:
            payload (object): Payload object to test.
            ptype (int, optional): Type of payload, default is 0.

        Returns:
            tuple: A tuple containing the result code, response, and payload object.
        """
        te_payload = deepcopy(payload)
        if (self._vhost == ""):
            te_payload.host = self._host
        else:
            te_payload.host = self._vhost
        te_payload.method = self._method
        te_payload.endpoint = self._endpoint

        if len(self._cookies) > 0:
            te_payload.header += "Cookie: " + ''.join(self._cookies) + "\r\n"

        if not ptype:
            te_payload.cl = 4
        else:
            te_payload.cl = 11

        te_payload.body = Chunked("Z")+EndChunk
        return self._test(te_payload)

    def _create_exec_test(self, name, te_payload):
        """
        Create an execution test for the given payload.

        Args:
            name (str): Name of the test.
            te_payload (object): Payload object to test.

        Returns:
            bool: True if the test was successful, False otherwise.
        """
        def pretty_print(name, dismsg):
            spacing = 13
            sys.stdout.write("\r"+" "*100+"\r")
            msg = Style.BRIGHT + Fore.MAGENTA + "[%s]%s: %s" % \
            (Fore.CYAN + name + Fore.MAGENTA, " "*(spacing-len(name)), dismsg)
            sys.stdout.write(CF(msg + Style.RESET_ALL))
            sys.stdout.flush()

            if dismsg[-1] == "\n":
                ansi_escape = re.compile(r'\x1B[@-_][0-?]*[ -/]*[@-~]')
                plaintext = ansi_escape.sub('', msg)
                if self._logh is not None:
                    self._logh.write(plaintext)
                    self._logh.flush()

        start_time = time.time()
        tecl_res = self._check_tecl(te_payload, 0)
        tecl_time = time.time()-start_time

        start_time = time.time()
        clte_res = self._check_clte(te_payload, 0)
        clte_time = time.time()-start_time

        if (clte_res[0] == 1):
            clte_res2 = self._check_clte(te_payload, 1)
            if clte_res2[0] == 0:
                self._attempts += 1
                if (self._attempts < 1):
                    return self._create_exec_test(name, te_payload)
                else:
                    dismsg = Fore.RED + "Potential CLTE Issue Found" + Fore.MAGENTA + " - " + Fore.CYAN + self._method + Fore.MAGENTA + " @ " + Fore.CYAN + ["http://","https://",][self.ssl_flag]+ self._host + self._endpoint + Fore.MAGENTA + " - " + Fore.CYAN + self._configfile.split('/')[-1] + "\n"
                    sys.stdout.write("START\n")
                    nn_new = str(clte_res2[2]).replace("\r\n", "\n")
                    sys.stdout.write(nn_new)
                    sys.stdout.write("END\n")
                    self._attempts = 0
                    return True

        elif (tecl_res[0] == 1):
            tecl_res2 = self._check_tecl(te_payload, 1)
            if tecl_res2[0] == 0:
                self._attempts += 1
                if (self._attempts < 3):
                    return self._create_exec_test(name, te_payload)
                else:
                    dismsg = Fore.RED + "Potential TECL Issue Found" + Fore.MAGENTA + " - " + Fore.CYAN + self._method + Fore.MAGENTA + " @ " + Fore.CYAN + ["http://","https://",][self.ssl_flag]+ self._host + self._endpoint + Fore.MAGENTA + " - " + Fore.CYAN + self._configfile.split('/')[-1] + "\n"
                    sys.stdout.write("START\n")
                    nn_new1 = str(tecl_res2[2]).replace("\r\n", "\n")
                    sys.stdout.write(nn_new1)
                    sys.stdout.write("\nEND\n")
                    self._attempts = 0
                    return True

        elif ((tecl_res[0] == 2) or (clte_res[0] == 2)):
            dismsg = Fore.YELLOW + "DISCONNECTED" + ["\n", ""][self._quiet]
            pretty_print(name, dismsg)

        elif ((tecl_res[0] == 0) and (clte_res[0] == 0)):
            tecl_msg = (Fore.MAGENTA + " (TECL: " + Fore.CYAN +"%.2f" + Fore.MAGENTA + " - " + \
            Fore.CYAN +"%s" + Fore.MAGENTA + ")") % (tecl_time, tecl_res[1][9:9+3])

            clte_msg = (Fore.MAGENTA + " (CLTE: " + Fore.CYAN +"%.2f" + Fore.MAGENTA + " - " + \
            Fore.CYAN +"%s" + Fore.MAGENTA + ")") % (clte_time, clte_res[1][9:9+3])

            dismsg = Fore.GREEN + "OK" + tecl_msg + clte_msg + ["\n", ""][self._quiet]
            pretty_print(name, dismsg)

        self._attempts = 0
        return False

def process_uri(uri):
    """
    Process the given URI and return the host, port, path, and SSL flag.

    Args:
        uri (str): URI to process.

    Returns:
        tuple: A tuple containing the host, port, path, and SSL flag.
    """
    u = urlparse(uri)

    if u.scheme == "https":
        ssl_flag = True
        std_port = 443
    elif u.scheme == "http":
        ssl_flag = False
        std_port = 80
    else:
        print_info("Error malformed URL not supported: %s" % (Fore.CYAN + uri))
        exit(1)

    if u.port:
        return (u.hostname, u.port, u.path, ssl_flag)
    else:
        return (u.hostname, std_port, u.path, ssl_flag)

def CF(text):
    """
    Color formatting function.

    Args:
        text (str): Text to format.

    Returns:
        str: Formatted text.
    """
    global NOCOLOR
    if NOCOLOR:
        ansi_escape = re.compile(r'\x1B[@-_][0-?]*[ -/]*[@-~]')
        text = ansi_escape.sub('', text)
    return text

def banner(sm_version):
    """
    Print the banner with the given version.

    Args:
        sm_version (str): Version of the smuggler.
    """
    print(CF(Fore.CYAN))
    print(CF(r"  _    _   _____    _____    _____          _              _              "))
    print(CF(r" | |  | | |  __ \  / ____|  |  __ \        | |            | |             "))
    print(CF(r" | |__| | | |__) || (___    | |  | |  ___  | |_   ___  ___| |_  ___  _ __ "))
    print(CF(r" |  __  | |  _  /  \___ \   | |  | | / _ \ | __| / _ \/ __| __|/ _ \| '__|"))
    print(CF(r" | |  | | | | \ \  ____) |  | |__| ||  __/ | |_ |  __/ (__| |_| (_) | |   "))
    print(CF(r" |_|  |_| |_|  \_\|_____/   |_____/  \___|  \__| \___|\___|\__|\___/|_|   "))
    print(CF(r""))
    print(CF(r"     @bihari_web                                                   %s"%(sm_version)))
    print(CF(Style.RESET_ALL))

def print_info(msg, file_handle=None):
    """
    Print information message.

    Args:
        msg (str): Message to print.
        file_handle (file, optional): File handle to write to, default is None.
    """
    ansi_escape = re.compile(r'\x1B[@-_][0-?]*[ -/]*[@-~]')
    msg = Style.BRIGHT + Fore.MAGENTA + "[%s] %s"%(Fore.CYAN+'+'+Fore.MAGENTA, msg) + Style.RESET_ALL
    plaintext = ansi_escape.sub('', msg)
    print(CF(msg))
    if file_handle is not None:
        file_handle.write(plaintext+"\n")

if __name__ == "__main__":
    global NOCOLOR
    if sys.version_info < (3, 0):
        print("Error: Smuggler requires Python 3.x")
        sys.exit(1)

    Parser = argparse.ArgumentParser()
    Parser.add_argument('-u', '--url', help="Target URL with Endpoint")
    Parser.add_argument('-v', '--vhost', default="", help="Specify a virtual host")
    Parser.add_argument('-x', '--exit_early', action='store_true',help="Exit scan on first finding")
    Parser.add_argument('-m', '--method', default="POST", help="HTTP method to use (e.g GET, POST) Default: POST")
    Parser.add_argument('-l', '--log', help="Specify a log file")
    Parser.add_argument('-q', '--quiet', action='store_true', help="Quiet mode will only log issues found")
    Parser.add_argument('-t', '--timeout', default=5.0, help="Socket timeout value Default: 5")
    Parser.add_argument('--no-color', action='store_true', help="Suppress color codes")
    Parser.add_argument('-c', '--configfile', default="default.py", help="Filepath to the configuration file of payloads")
    Args = Parser.parse_args()

    NOCOLOR = Args.no_color
    if os.name == 'nt':
        NOCOLOR = True

    Version = "v1.0"
    banner(Version)

    if sys.version_info < (3, 0):
        print_info("Error: Smuggler requires Python 3.x")
        sys.exit(1)

    if Args.url is None:
        if sys.stdin.isatty():
            print_info("Error: no direct URL or piped URL specified\n")
            Parser.print_help()
            exit(1)
        Servers = sys.stdin.read().split("\n")
    else:
        Servers = [Args.url + " " + Args.method]

    FileHandle = None
    if Args.log is not None:
        try:
            FileHandle = open(Args.log, "w")
        except:
            print_info("Error: Issue with log file destination")
            print(Parser.print_help())
            sys.exit(1)

    for server in Servers:
        if server == "":
            continue
        server = server.split(" ")

        if len(server) == 1:
            server += [Args.method]

        if server[0].lower().strip()[0:4] != "http":
            server[0] = "https://" + server[0]

        host, port, endpoint, SSLFlagval = process_uri(server[0])
        method = server[1].upper()
        configfile = Args.configfile

        print_info("URL        : %s"%(Fore.CYAN + server[0]), FileHandle)
        print_info("Method     : %s"%(Fore.CYAN + method), FileHandle)
        print_info("Endpoint   : %s"%(Fore.CYAN + endpoint), FileHandle)
        print_info("Configfile : %s"%(Fore.CYAN + configfile), FileHandle)
        print_info("Timeout    : %s"%(Fore.CYAN + str(float(Args.timeout)) + Fore.MAGENTA + " seconds"), FileHandle)

        sm = Desyncr(configfile, host, port, url=server[0], method=method, endpoint=endpoint, SSLFlag=SSLFlagval, logh=FileHandle, smargs=Args)
        sm.run()

    if FileHandle is not None:
        FileHandle.close()
