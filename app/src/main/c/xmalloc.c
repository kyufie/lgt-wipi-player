/*
 * A horribly inefficient memory allocator based on mmap that is more likely
 * to return "positive" memory address than malloc
 * The functions provided can be used as a drop in replacement for malloc and free
 */

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/mman.h>

#define MEM_BASE 0x8000
#define ALIGN(val, align) ((val) & (align - 1) ? (val + align) & ~(align - 1) : (val))
#define pagesize sysconf(_SC_PAGESIZE)

#define MAP_MAGIC "MAPP"
#define MAP_MAGIC_LEN 4

void *base = (void*) MEM_BASE;

struct map_hdr {
  char magic[MAP_MAGIC_LEN];
  uint32_t size;
};

/*
 * Of course, with enough allocation, the base address will eventually
 * grow so much, to the point where xmalloc starts spitting out negative
 * addresses.
 * But for our use case, this is fine.
 */
void *xmalloc(size_t size)
{
  void *map;
  unsigned long map_size;
  struct map_hdr *hdr;

  map_size = ALIGN(size, pagesize);
  map = mmap(base, map_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0);
  if (map == MAP_FAILED)
    return NULL;

  hdr = (struct map_hdr*) map;
  memcpy(&hdr->magic, "MAPP", 4);
  hdr->size = map_size;

  base = map + map_size;

  return (void*)((uintptr_t) map + sizeof(*hdr));
}

void xfree(void *ptr)
{
  struct map_hdr *hdr;

  hdr = (struct map_hdr*) ptr;
  if (memcmp(hdr->magic, MAP_MAGIC, MAP_MAGIC_LEN))
    return;

  munmap(ptr, hdr->size);
}

/*
 * Wrappers to override system functions
 */
void *malloc(size_t size)
{
  return xmalloc(size);
}

void free(void *ptr)
{
  xfree(ptr);
}
