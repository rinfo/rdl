import urllib
import sys
import feedparser
import os
import errno
import socket

socket.setdefaulttimeout(10)

if len(sys.argv) != 3:
    print('Feed parser and downloader v1.0')
    print('Usage: feed.py [base url] [relative start atom feed url]')
    quit()

base_url = sys.argv[1]
parse_url = base_url+sys.argv[2]

list_of_urls_to_download_structure=list()

def add_to_list(rel_url):
    if rel_url.startswith('/'):
	rel_url = rel_url[1:]
    if rel_url.startswith(base_url):
        list_of_urls_to_download_structure.append(rel_url)
    else:    
        list_of_urls_to_download_structure.append(base_url+rel_url)

def make_sure_directory_exists(file_name_and_path):
    try:
        os.makedirs(os.path.abspath(os.path.dirname(file_name_and_path)))
    except OSError, exc:
        if exc.errno != errno.EEXIST:
            raise
    return file_name_and_path

def replace_in_file(file_name_and_path, text_to_search, text_to_replace_with):
    tmp_file_name = file_name_and_path+'.tmp'
    read_file = open(file_name_and_path, 'r')
    write_file = open(tmp_file_name, 'w')
    for line in read_file:
        write_file.write(line.replace(text_to_search, text_to_replace_with))
    read_file.close()
    write_file.close()
    os.remove(file_name_and_path)
    os.rename(tmp_file_name, file_name_and_path)

def create_html_dummy_file(file_name):
    write_file = open(file_name, 'w')
    write_file.write('<html><body>Missing</body></html>')
    write_file.close()

# Go throu atom feed and store all links
while parse_url:
    print ('Processing ' + parse_url)
    add_to_list(parse_url)
    d = feedparser.parse(parse_url)
#    for entry in d.entries: 
#        if hasattr(entry.content[0], 'src'):
#	    if entry.content[0].src:
#                add_to_list(entry.content[0].src)
#        if hasattr(entry, 'links'):
#            for link in entry.links:
#                if link.rel in ['alternate','enclosure']:
#                    add_to_list(link.href)
    parse_url = ''
    if hasattr(d.feed, 'links'):
        for link in d.feed.links:
            if hasattr(link, 'href'):
                if link.rel == 'prev-archive':
                    if (link.href.startswith(base_url)):
                        parse_url = link.href
                    else:
                        parse_url = base_url+link.href
    else:
        print ('Missing links cannot continue traversing atom feed')

socket.setdefaulttimeout(10)

# Download all links in to usable structure
print ('Found %i item(s)' % len(list_of_urls_to_download_structure) )
for item in list_of_urls_to_download_structure:
    print ('Downloading '+item)
    try:
        local_filename_and_path = item[len(base_url):]
        local_filename = os.path.basename(local_filename_and_path)
        urllib.urlretrieve(item, make_sure_directory_exists(local_filename))    
        if local_filename_and_path.startswith('atom'):
            replace_in_file(local_filename, base_url+'atom/', '')
            replace_in_file(local_filename, base_url+'rdf/', '')
            replace_in_file(local_filename, base_url+'html/', '')
    except IOError:
        print ('IOError: failed to download %s' % item)
        if local_filename.endswith('.html'):
            create_html_dummy_file(local_filename)
